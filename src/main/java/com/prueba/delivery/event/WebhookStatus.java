package com.prueba.delivery.event;

public enum WebhookStatus {
    PENDING,
    EXTERNAL_CALL_IN_PROGRESS,
    EXTERNAL_CALL_RETRY,
    PROCESSING,
    SUCCESS,
    FAILED_TRANSIENT,
    FAILED_PERMANENT,
    FAILED
}
