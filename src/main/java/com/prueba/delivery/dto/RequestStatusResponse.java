package com.prueba.delivery.dto;

import com.prueba.delivery.entity.WebhookLog;
import com.prueba.delivery.entity.WebhookResponse;
import com.prueba.delivery.event.WebhookStatus;
import java.time.Instant;
import java.util.List;

public class RequestStatusResponse {

    private Long responseId;
    private String eventType;
    private String source;
    private String fileName;
    private String fileType;
    private WebhookStatus state;
    private Instant receivedAt;
    private String message;
    private List<WebhookLogEntry> logs;

    public RequestStatusResponse() {
    }

    public RequestStatusResponse(WebhookResponse response, List<WebhookLog> logs) {
        this.responseId = response.getId();
        this.eventType = response.getEventType();
        this.source = response.getSource();
        this.fileName = response.getFileName();
        this.fileType = response.getFileType();
        this.state = response.getState();
        this.receivedAt = response.getReceivedAt();
        this.message = response.getMessage();
        this.logs = logs.stream()
                .map(WebhookLogEntry::fromEntity)
                .toList();
    }

    // Getters y Setters
    public Long getResponseId() {
        return responseId;
    }

    public void setResponseId(Long responseId) {
        this.responseId = responseId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public WebhookStatus getState() {
        return state;
    }

    public void setState(WebhookStatus state) {
        this.state = state;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<WebhookLogEntry> getLogs() {
        return logs;
    }

    public void setLogs(List<WebhookLogEntry> logs) {
        this.logs = logs;
    }

    // Clase interna para los logs
    public static class WebhookLogEntry {
        private Long id;
        private Instant timestamp;
        private String eventType;
        private String source;
        private String action;
        private String message;
        private Integer statusCode;
        private String payloadSummary;

        public WebhookLogEntry() {
        }

        public WebhookLogEntry(Long id, Instant timestamp, String eventType, String source,
                               String action, String message, Integer statusCode, String payloadSummary) {
            this.id = id;
            this.timestamp = timestamp;
            this.eventType = eventType;
            this.source = source;
            this.action = action;
            this.message = message;
            this.statusCode = statusCode;
            this.payloadSummary = payloadSummary;
        }

        public static WebhookLogEntry fromEntity(WebhookLog log) {
            return new WebhookLogEntry(
                    log.getId(),
                    log.getTimestamp(),
                    log.getEventType(),
                    log.getSource(),
                    log.getAction(),
                    log.getMessage(),
                    log.getStatusCode(),
                    log.getPayloadSummary()
            );
        }

        // Getters y Setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Instant getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Instant timestamp) {
            this.timestamp = timestamp;
        }

        public String getEventType() {
            return eventType;
        }

        public void setEventType(String eventType) {
            this.eventType = eventType;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Integer getStatusCode() {
            return statusCode;
        }

        public void setStatusCode(Integer statusCode) {
            this.statusCode = statusCode;
        }

        public String getPayloadSummary() {
            return payloadSummary;
        }

        public void setPayloadSummary(String payloadSummary) {
            this.payloadSummary = payloadSummary;
        }
    }
}
