package com.prueba.delivery.listener;

import com.prueba.delivery.entity.WebhookLog;
import com.prueba.delivery.event.DeliveryEvent;
import com.prueba.delivery.event.WebhookPayload;
import com.prueba.delivery.event.WebhookStatus;
import com.prueba.delivery.repository.WebhookLogRepository;
import com.prueba.delivery.service.WebhookResponseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class DeliveryEventListener {

    private static final Logger log = LoggerFactory.getLogger(DeliveryEventListener.class);
    private final WebhookResponseService webhookResponseService;
    private final WebhookLogRepository webhookLogRepository;

    public DeliveryEventListener(WebhookResponseService webhookResponseService,
                                 WebhookLogRepository webhookLogRepository) {
        this.webhookResponseService = webhookResponseService;
        this.webhookLogRepository = webhookLogRepository;
    }

    @Async("eventExecutor")
    @EventListener
    public void handleDeliveryEvent(DeliveryEvent event) {
        WebhookPayload payload = event.getPayload();
        payload.setState(WebhookStatus.PENDING);
        webhookResponseService.updateState(payload.getResponseId(), WebhookStatus.PROCESSING, "En procesamiento");

        // Log de inicio de procesamiento
        WebhookLog processingLog = new WebhookLog(
                Instant.now(),
                payload.getEventType(),
                payload.getSource(),
                "PROCESSING",
                "Iniciando procesamiento del archivo: " + payload.getFileName(),
                null,
                payload.getResponseId(),
                null
        );
        //webhookLogRepository.save(processingLog);

        log.info("Procesando evento interno: type={} source={} receivedAt={} state={} fileName={} fileType={} size={}",
                payload.getEventType(),
                payload.getSource(),
                payload.getReceivedAt(),
                payload.getState(),
                payload.getFileName(),
                payload.getFileType(),
                payload.getFileContent() != null ? payload.getFileContent().length : 0);

        try {
            Thread.sleep(200);
            payload.setState(WebhookStatus.SUCCESS);
            webhookResponseService.updateState(payload.getResponseId(), WebhookStatus.SUCCESS, "Procesado exitosamente");

            // Log de éxito
            WebhookLog successLog = new WebhookLog(
                    Instant.now(),
                    payload.getEventType(),
                    payload.getSource(),
                    "SUCCESS",
                    "Procesamiento completado exitosamente",
                    null,
                    payload.getResponseId(),
                    null
            );
            webhookLogRepository.save(successLog);

            log.info("Evento procesado exitosamente: {}", payload.getState());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            payload.setState(WebhookStatus.FAILED);
            webhookResponseService.updateState(payload.getResponseId(), WebhookStatus.FAILED, "Procesamiento interrumpido");

            // Log de fallo por interrupción
            WebhookLog failedLog = new WebhookLog(
                    Instant.now(),
                    payload.getEventType(),
                    payload.getSource(),
                    "FAILED",
                    "Procesamiento interrumpido",
                    null,
                    payload.getResponseId(),
                    null
            );
            webhookLogRepository.save(failedLog);

            log.warn("Procesamiento del evento fue interrumpido", interrupted);
        } catch (Exception ex) {
            payload.setState(WebhookStatus.FAILED);
            webhookResponseService.updateState(payload.getResponseId(), WebhookStatus.FAILED, "Error en el procesamiento");

            // Log de fallo por excepción
            WebhookLog failedLog = new WebhookLog(
                    Instant.now(),
                    payload.getEventType(),
                    payload.getSource(),
                    "FAILED",
                    "Error en el procesamiento: " + ex.getMessage(),
                    null,
                    payload.getResponseId(),
                    null
            );
            webhookLogRepository.save(failedLog);

            log.error("Error procesando evento interno", ex);
        }
    }
}
