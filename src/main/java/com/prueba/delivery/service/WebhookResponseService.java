package com.prueba.delivery.service;

import com.prueba.delivery.entity.WebhookResponse;
import com.prueba.delivery.event.WebhookPayload;
import com.prueba.delivery.event.WebhookStatus;
import com.prueba.delivery.repository.WebhookResponseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class WebhookResponseService {

    private final WebhookResponseRepository repository;

    public WebhookResponseService(WebhookResponseRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public WebhookResponse createPendingResponse(WebhookPayload payload) {
        WebhookResponse response = new WebhookResponse(
                payload.getEventType(),
                payload.getSource(),
                payload.getFileName(),
                payload.getFileType(),
                payload.getFileContent(),
                WebhookStatus.PENDING,
                payload.getReceivedAt(),
                "Solicitud recibida"
        );
        return repository.save(response);
    }

    @Transactional
    public void updateState(Long id, WebhookStatus state, String message) {
        Optional<WebhookResponse> optional = repository.findById(id);
        if (optional.isPresent()) {
            WebhookResponse response = optional.get();
            response.setState(state);
            response.setMessage(message);
            response.setReceivedAt(response.getReceivedAt());
            repository.save(response);
        }
    }
}
