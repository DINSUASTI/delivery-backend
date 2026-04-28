package com.prueba.delivery.controller;

import com.prueba.delivery.dto.RequestStatusResponse;
import com.prueba.delivery.entity.WebhookLog;
import com.prueba.delivery.entity.WebhookResponse;
import com.prueba.delivery.event.WebhookPayload;
import com.prueba.delivery.event.WebhookStatus;
import com.prueba.delivery.repository.WebhookLogRepository;
import com.prueba.delivery.repository.WebhookResponseRepository;
import com.prueba.delivery.service.ExternalCallService;
import com.prueba.delivery.service.ExternalCallService.ExternalCallResult;
import com.prueba.delivery.service.EventPublisherService;
import com.prueba.delivery.service.WebhookResponseService;
import com.prueba.delivery.util.IdempotencyKeyGenerator;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/webhook")
public class WebhookController {

    private final EventPublisherService eventPublisherService;
    private final WebhookResponseService webhookResponseService;
    private final WebhookLogRepository webhookLogRepository;
    private final WebhookResponseRepository webhookResponseRepository;
    private final ExternalCallService externalCallService;

    public WebhookController(EventPublisherService eventPublisherService,
                             WebhookResponseService webhookResponseService,
                             WebhookLogRepository webhookLogRepository,
                             WebhookResponseRepository webhookResponseRepository,
                             ExternalCallService externalCallService) {
        this.eventPublisherService = eventPublisherService;
        this.webhookResponseService = webhookResponseService;
        this.webhookLogRepository = webhookLogRepository;
        this.webhookResponseRepository = webhookResponseRepository;
        this.externalCallService = externalCallService;
    }

    @PostMapping(value = "/test", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> receiveTestWebhook(
            @RequestPart("file") MultipartFile file,
            @RequestPart(name = "eventType", required = false) String eventType,
            @RequestPart(name = "source", required = false) String source
    ) throws IOException {
        // Preparar datos de la solicitud
        String finalEventType = eventType != null ? eventType : "PDF_UPLOAD";
        String finalSource = source != null ? source : "pdf-webhook";
        String fileName = file.getOriginalFilename();
        String fileType = file.getContentType();
        byte[] fileContent = file.getBytes();

        // Generar clave de idempotencia
        String idempotencyKey = IdempotencyKeyGenerator.generateKey(
                finalEventType, finalSource, fileName, fileType, fileContent);

        // Verificar si ya existe una solicitud con la misma clave
        Optional<WebhookResponse> existingResponse = webhookResponseRepository.findByIdempotencyKey(idempotencyKey);
        if (existingResponse.isPresent()) {
            WebhookResponse existing = existingResponse.get();

            // Log de solicitud duplicada
            WebhookLog duplicateLog = new WebhookLog(
                    Instant.now(),
                    finalEventType,
                    finalSource,
                    "DUPLICATE_REQUEST",
                    "Solicitud duplicada detectada. ID existente: " + existing.getId(),
                    409,
                    existing.getId(),
                    "Archivo: " + fileName + ", Tipo: " + fileType + ", Key: " + idempotencyKey
            );
            webhookLogRepository.save(duplicateLog);

            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "status", "DUPLICATE_REQUEST",
                    "responseId", existing.getId(),
                    "eventType", existing.getEventType(),
                    "fileName", existing.getFileName(),
                    "fileType", existing.getFileType(),
                    "state", existing.getState().name(),
                    "receivedAt", existing.getReceivedAt().toString(),
                    "message", "Esta solicitud ya fue procesada anteriormente",
                    "idempotencyKey", idempotencyKey
            ));
        }

        WebhookPayload payload = new WebhookPayload();
        payload.setEventType(finalEventType);
        payload.setSource(finalSource);
        payload.setReceivedAt(Instant.now());
        payload.setState(WebhookStatus.PENDING);
        payload.setFileName(fileName);
        payload.setFileType(fileType);
        payload.setFileContent(fileContent);

        var response = webhookResponseService.createPendingResponse(payload);
        // Establecer la clave de idempotencia en la respuesta
        response.setIdempotencyKey(idempotencyKey);
        webhookResponseRepository.save(response);

        payload.setResponseId(response.getId());

        // Log de recepción del webhook
        WebhookLog logEntry = new WebhookLog(
                Instant.now(),
                payload.getEventType(),
                payload.getSource(),
                "RECEIVED",
                "Solicitud recibida con archivo: " + payload.getFileName(),
                0,
                payload.getResponseId(),
                "Archivo: " + payload.getFileName() + ", Tipo: " + payload.getFileType()
        );
        webhookLogRepository.save(logEntry);

        // Llamada a servicio externo con reintentos y manejo de estados
        ExternalCallResult externalCallResult = externalCallService.executeExternalCall(payload);

        if (externalCallResult.finalState() == WebhookStatus.PROCESSING) {
            eventPublisherService.publish(payload);
            return ResponseEntity.accepted().body(Map.of(
                    "status", externalCallResult.finalState().name(),
                    "responseId", payload.getResponseId(),
                    "eventType", payload.getEventType(),
                    "fileName", payload.getFileName(),
                    "fileType", payload.getFileType(),
                    "receivedAt", payload.getReceivedAt().toString(),
                    "attempts", externalCallResult.attempts(),
                    "message", externalCallResult.message()
            ));
        }

        HttpStatus returnStatus = externalCallResult.finalState() == WebhookStatus.FAILED_PERMANENT
                ? HttpStatus.BAD_REQUEST
                : HttpStatus.SERVICE_UNAVAILABLE;

        return ResponseEntity.status(returnStatus).body(Map.of(
                "status", externalCallResult.finalState().name(),
                "responseId", payload.getResponseId(),
                "eventType", payload.getEventType(),
                "fileName", payload.getFileName(),
                "fileType", payload.getFileType(),
                "receivedAt", payload.getReceivedAt().toString(),
                "attempts", externalCallResult.attempts(),
                "message", externalCallResult.message()
        ));
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<?> getRequestStatus(@PathVariable Long id) {
        WebhookResponse response = webhookResponseRepository.findById(id)
                .orElse(null);

        if (response == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "NOT_FOUND",
                    "message", "Solicitud con ID " + id + " no encontrada"
            ));
        }

        var logs = webhookLogRepository.findByResponseIdOrderByIdAsc(id);
        RequestStatusResponse statusResponse = new RequestStatusResponse(response, logs);

        return ResponseEntity.ok(statusResponse);
    }
}

