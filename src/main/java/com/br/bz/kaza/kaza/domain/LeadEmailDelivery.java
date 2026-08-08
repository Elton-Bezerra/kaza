package com.br.bz.kaza.kaza.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "lead_email_outbox")
public class LeadEmailDelivery {
    @Id
    @GeneratedValue
    private UUID id;
    @Column(name = "lead_id", nullable = false)
    private UUID leadId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private LeadEmailDeliveryType type;
    @Column(name = "from_address", nullable = false)
    private String fromAddress;
    @Column(nullable = false)
    private String recipient;
    @Column(nullable = false)
    private String subject;
    @Column(nullable = false, columnDefinition = "text")
    private String body;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LeadEmailDeliveryStatus status = LeadEmailDeliveryStatus.PENDING;
    @Column(nullable = false)
    private int attempts;
    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;
    @Column(name = "sent_at")
    private OffsetDateTime sentAt;
    @Column(nullable = false)
    private OffsetDateTime createdAt;
    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    protected LeadEmailDelivery() {
    }

    public LeadEmailDelivery(UUID leadId, LeadEmailDeliveryType type, String fromAddress,
            String recipient, String subject, String body) {
        this.leadId = leadId;
        this.type = type;
        this.fromAddress = fromAddress;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
    }

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public void recordAttempt() {
        attempts++;
    }

    public void markSent() {
        status = LeadEmailDeliveryStatus.SENT;
        lastError = null;
        sentAt = OffsetDateTime.now();
    }

    public void markFailed(String error) {
        status = LeadEmailDeliveryStatus.FAILED;
        lastError = error;
    }

    public UUID getId() {
        return id;
    }

    public UUID getLeadId() {
        return leadId;
    }

    public LeadEmailDeliveryType getType() {
        return type;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public LeadEmailDeliveryStatus getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public String getLastError() {
        return lastError;
    }

    public OffsetDateTime getSentAt() {
        return sentAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
