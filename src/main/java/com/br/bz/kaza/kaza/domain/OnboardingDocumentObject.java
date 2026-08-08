package com.br.bz.kaza.kaza.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "onboarding_document_objects")
public class OnboardingDocumentObject {
    @Id
    @Column(name = "storage_key")
    private UUID storageKey;
    @Column(nullable = false)
    private byte[] content;
    @Column(nullable = false)
    private OffsetDateTime createdAt;

    protected OnboardingDocumentObject() {
    }

    public OnboardingDocumentObject(UUID storageKey, byte[] content) {
        this.storageKey = storageKey;
        this.content = content;
    }

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public byte[] getContent() {
        return content;
    }
}
