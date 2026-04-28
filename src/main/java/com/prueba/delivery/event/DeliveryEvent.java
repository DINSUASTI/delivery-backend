package com.prueba.delivery.event;

import org.springframework.context.ApplicationEvent;

public class DeliveryEvent extends ApplicationEvent {

    private final WebhookPayload payload;

    public DeliveryEvent(Object source, WebhookPayload payload) {
        super(source);
        this.payload = payload;
    }

    public WebhookPayload getPayload() {
        return payload;
    }
}
