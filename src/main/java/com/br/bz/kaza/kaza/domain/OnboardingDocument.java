package com.br.bz.kaza.kaza.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "onboarding_documents")
public class OnboardingDocument {
    @Id
    @GeneratedValue
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private OnboardingApplication application;
    @Column(name = "storage_key", nullable = false, unique = true)
    private UUID storageKey;
    @Column(name = "original_filename", nullable = false)
    private String originalFilename;
    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;
    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;
    @Column(nullable = false, length = 64)
    private String sha256;
    @Enumerated(EnumType.STRING)
    @Column(name = "scan_status", nullable = false, length = 32)
    private ScanStatus scanStatus = ScanStatus.PENDING;
    @Enumerated(EnumType.STRING)
    @Column(name = "retention_state", nullable = false, length = 32)
    private RetentionState retentionState = RetentionState.ACTIVE;
    @Column(name = "uploaded_at", nullable = false)
    private OffsetDateTime uploadedAt;
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    protected OnboardingDocument() {
    }

    public OnboardingDocument(OnboardingApplication application, UUID storageKey,
            String originalFilename, String contentType, long sizeBytes, String sha256) {
        this.application = application;
        this.storageKey = storageKey;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.sha256 = sha256;
    }

    @PrePersist
    void onCreate() {
        uploadedAt = OffsetDateTime.now();
    }

    public void markDeleted() {
        if (retentionState == RetentionState.DELETED) {
            return;
        }
        retentionState = RetentionState.DELETED;
        deletedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public OnboardingApplication getApplication() {
        return application;
    }

    public UUID getStorageKey() {
        return storageKey;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getSha256() {
        return sha256;
    }

    public ScanStatus getScanStatus() {
        return scanStatus;
    }

    public RetentionState getRetentionState() {
        return retentionState;
    }

    public OffsetDateTime getUploadedAt() {
        return uploadedAt;
    }

    public enum ScanStatus {
        PENDING, CLEAN, INFECTED, FAILED
    }

    public enum RetentionState {
        ACTIVE, DELETED
    }
}
