package com.prueba.delivery.service;

import com.prueba.delivery.entity.WebhookLog;
import com.prueba.delivery.event.WebhookPayload;
import com.prueba.delivery.event.WebhookStatus;
import com.prueba.delivery.repository.WebhookLogRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class ExternalCallService {

    private final RestTemplate restTemplate;
    private final WebhookLogRepository webhookLogRepository;
    private final WebhookResponseService webhookResponseService;
    private final String externalCallUrl;
    private static final int MAX_RETRIES = 10;
    private static final long RETRY_DELAY_MS = 5000;

    public ExternalCallService(RestTemplate restTemplate,
                               WebhookLogRepository webhookLogRepository,
                               WebhookResponseService webhookResponseService,
                               @Value("${external.call.url}") String externalCallUrl) {
        this.restTemplate = restTemplate;
        this.webhookLogRepository = webhookLogRepository;
        this.webhookResponseService = webhookResponseService;
        this.externalCallUrl = externalCallUrl;
    }

    @Transactional
    public ExternalCallResult executeExternalCall(WebhookPayload payload) {
        Long responseId = payload.getResponseId();
        webhookResponseService.updateState(responseId, WebhookStatus.EXTERNAL_CALL_IN_PROGRESS, "Llamada externa iniciada");
        saveAudit(payload, "EXT_CALL_START", "Inicio request plataforma externa", null);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("eventType", payload.getEventType());
        requestBody.put("source", payload.getSource());
        requestBody.put("fileName", payload.getFileName());
        requestBody.put("fileType", payload.getFileType());

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody);
                ResponseEntity<Map> response = restTemplate.postForEntity(externalCallUrl, requestEntity, Map.class);
                int statusCode = response.getStatusCode().value();
                String message = "Respuesta plataforma externa " + statusCode + " en intento " + attempt;
                saveAudit(payload, "EXT_CALL_ATTEMPT", message, statusCode);

                if (response.getStatusCode() == HttpStatus.OK) {
                    webhookResponseService.updateState(responseId, WebhookStatus.PROCESSING, "Llamada externa exitosa");
                    saveAudit(payload, "EXT_CALL_SUCCESS", "Llamada externa finalizada con éxito"+ " en intento " + attempt, statusCode);
                    return new ExternalCallResult(WebhookStatus.PROCESSING, statusCode, attempt, message);
                }

                if (isTransientError(statusCode)) {
                    if (attempt == MAX_RETRIES) {
                        String finalMessage = "Error transitorio persistente después de " + attempt + " intentos";
                        webhookResponseService.updateState(responseId, WebhookStatus.FAILED_TRANSIENT, finalMessage);
                        saveAudit(payload, "EXT_CALL_FINAL_TRANSIENT_FAILURE", finalMessage, statusCode);
                        return new ExternalCallResult(WebhookStatus.FAILED_TRANSIENT, statusCode, attempt, finalMessage);
                    }
                    webhookResponseService.updateState(responseId, WebhookStatus.EXTERNAL_CALL_RETRY, "Reintentando error transitorio " + statusCode + " (intento " + attempt + ")");
                    saveAudit(payload, "EXT_CALL_RETRY", "Error transitorio " + statusCode + ". Reintentando...", statusCode);
                    Thread.sleep(RETRY_DELAY_MS);
                    continue;
                }

                String finalMessage = "Error definitivo de llamada externa: " + statusCode;
                webhookResponseService.updateState(responseId, WebhookStatus.FAILED_PERMANENT, finalMessage);
                saveAudit(payload, "EXT_CALL_PERMANENT_FAILURE", finalMessage, statusCode);
                return new ExternalCallResult(WebhookStatus.FAILED_PERMANENT, statusCode, attempt, finalMessage);
            } catch (HttpServerErrorException ex) {
                int statusCode = ex.getStatusCode().value();
                if (attempt == MAX_RETRIES) {
                    String finalMessage = "Error transitorio 5xx persistente después de " + attempt + " intentos";
                    webhookResponseService.updateState(responseId, WebhookStatus.FAILED_TRANSIENT, finalMessage);
                    saveAudit(payload, "EXT_CALL_FINAL_TRANSIENT_FAILURE", finalMessage, statusCode);
                    return new ExternalCallResult(WebhookStatus.FAILED_TRANSIENT, statusCode, attempt, finalMessage);
                }
                webhookResponseService.updateState(responseId, WebhookStatus.EXTERNAL_CALL_RETRY, "Reintentando error 5xx (intento " + attempt + ")");
                saveAudit(payload, "EXT_CALL_RETRY", "Error servidor 5xx. Reintentando...", statusCode);
                sleep();
            } catch (HttpClientErrorException ex) {
                int statusCode = ex.getStatusCode().value();
                if (statusCode == 429) {
                    if (attempt == MAX_RETRIES) {
                        String finalMessage = "Error transitorio 429 persistente después de " + attempt + " intentos";
                        webhookResponseService.updateState(responseId, WebhookStatus.FAILED_TRANSIENT, finalMessage);
                        saveAudit(payload, "EXT_CALL_FINAL_TRANSIENT_FAILURE", finalMessage, statusCode);
                        return new ExternalCallResult(WebhookStatus.FAILED_TRANSIENT, statusCode, attempt, finalMessage);
                    }
                    webhookResponseService.updateState(responseId, WebhookStatus.EXTERNAL_CALL_RETRY, "Reintentando error 429 (intento " + attempt + ")");
                    saveAudit(payload, "EXT_CALL_RETRY", "Error 429. Reintentando...", statusCode);
                    sleep();
                    continue;
                }
                String finalMessage = "Error definitivo de cliente: " + statusCode;
                webhookResponseService.updateState(responseId, WebhookStatus.FAILED_PERMANENT, finalMessage);
                saveAudit(payload, "EXT_CALL_PERMANENT_FAILURE", finalMessage, statusCode);
                return new ExternalCallResult(WebhookStatus.FAILED_PERMANENT, statusCode, attempt, finalMessage);
            } catch (ResourceAccessException ex) {
                if (attempt == MAX_RETRIES) {
                    String finalMessage = "Error de conexión persistente después de " + attempt + " intentos";
                    webhookResponseService.updateState(responseId, WebhookStatus.FAILED_TRANSIENT, finalMessage);
                    saveAudit(payload, "EXT_CALL_FINAL_TRANSIENT_FAILURE", finalMessage, null);
                    return new ExternalCallResult(WebhookStatus.FAILED_TRANSIENT, -1, attempt, finalMessage);
                }
                webhookResponseService.updateState(responseId, WebhookStatus.EXTERNAL_CALL_RETRY, "Reintentando error de conexión (intento " + attempt + ")");
                saveAudit(payload, "EXT_CALL_RETRY", "Error de conexión. Reintentando...", null);
                sleep();
            } catch (Exception ex) {
                String finalMessage = "Error inesperado de llamada externa: " + ex.getMessage();
                webhookResponseService.updateState(responseId, WebhookStatus.FAILED_PERMANENT, finalMessage);
                saveAudit(payload, "EXT_CALL_PERMANENT_FAILURE", finalMessage, null);
                return new ExternalCallResult(WebhookStatus.FAILED_PERMANENT, -1, attempt, finalMessage);
            }
        }
        String finalMessage = "Fallo desconocido en la llamada externa";
        webhookResponseService.updateState(responseId, WebhookStatus.FAILED_PERMANENT, finalMessage);
        saveAudit(payload, "EXT_CALL_PERMANENT_FAILURE", finalMessage, null);
        return new ExternalCallResult(WebhookStatus.FAILED_PERMANENT, -1, MAX_RETRIES, finalMessage);
    }

    private boolean isTransientError(int statusCode) {
        return statusCode == 429 || (statusCode >= 500 && statusCode < 600);
    }

    private void saveAudit(WebhookPayload payload, String action, String message, Integer statusCode) {
        WebhookLog logEntry = new WebhookLog(
                Instant.now(),
                payload.getEventType(),
                payload.getSource(),
                action,
                message,
                statusCode,
                payload.getResponseId(),
                "Archivo: " + payload.getFileName() + ", Tipo: " + payload.getFileType()
        );
        webhookLogRepository.save(logEntry);
    }

    private void sleep() {
        try {
            Thread.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    public static record ExternalCallResult(WebhookStatus finalState, int statusCode, int attempts, String message) {
    }
}
