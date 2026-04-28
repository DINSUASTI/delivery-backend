package com.prueba.delivery.entity;

import com.prueba.delivery.event.WebhookStatus;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "webhook_response")
public class WebhookResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String source;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String fileType;

    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] fileContent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WebhookStatus state;

    @Column(nullable = false)
    private Instant receivedAt;

    @Column(nullable = true, unique = true, length = 64)
    private String idempotencyKey;

    @Column(nullable = true, length = 512)
    private String message;

    public WebhookResponse() {
    }

    public WebhookResponse(String eventType, String source, String fileName, String fileType,
                           byte[] fileContent, WebhookStatus state, Instant receivedAt, String message) {
        this.eventType = eventType;
        this.source = source;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileContent = fileContent;
        this.state = state;
        this.receivedAt = receivedAt;
        this.message = message;
    }

    public Long getId() {
        return id;
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

    public byte[] getFileContent() {
        return fileContent;
    }

    public void setFileContent(byte[] fileContent) {
        this.fileContent = fileContent;
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

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
