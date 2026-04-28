package com.prueba.delivery.service;

import com.prueba.delivery.event.DeliveryEvent;
import com.prueba.delivery.event.WebhookPayload;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class EventPublisherService {

    private final ApplicationEventPublisher publisher;

    public EventPublisherService(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publish(WebhookPayload payload) {
        publisher.publishEvent(new DeliveryEvent(this, payload));
    }
}
