package com.prueba.delivery.event;

import java.time.Instant;
import java.util.Map;

import com.prueba.delivery.event.WebhookStatus;

public class WebhookPayload {

    private String eventType;
    private String source;
    private Map<String, Object> data;
    private Instant receivedAt;
    private WebhookStatus state;

    public WebhookPayload() {
        this.state = WebhookStatus.PENDING;
    }

    private String fileName;
    private String fileType;
    private byte[] fileContent;

    public WebhookPayload(String eventType, String source, Map<String, Object> data, Instant receivedAt) {
        this.eventType = eventType;
        this.source = source;
        this.data = data;
        this.receivedAt = receivedAt;
        this.state = WebhookStatus.PENDING;
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

    public byte[] getFileContent() {
        return fileContent;
    }

    public void setFileContent(byte[] fileContent) {
        this.fileContent = fileContent;
    }

    private Long responseId;

    public Long getResponseId() {
        return responseId;
    }

    public void setResponseId(Long responseId) {
        this.responseId = responseId;
    }

    public WebhookStatus getState() {
        return state;
    }

    public void setState(WebhookStatus state) {
        this.state = state;
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

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }
}
