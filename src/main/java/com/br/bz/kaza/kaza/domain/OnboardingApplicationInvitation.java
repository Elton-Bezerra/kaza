package com.br.bz.kaza.kaza.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "onboarding_application_invitations")
public class OnboardingApplicationInvitation {
    @Id
    @GeneratedValue
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lead_id", nullable = false)
    private OnboardingLead lead;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;
    @Column(name = "applicant_subject", nullable = false, length = 200)
    private String applicantSubject;
    @Column(name = "application_id")
    private UUID applicationId;
    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;
    @Column(name = "accepted_at")
    private OffsetDateTime acceptedAt;
    @Column(nullable = false)
    private OffsetDateTime createdAt;
    @Column(nullable = false)
    private OffsetDateTime updatedAt;
    @Version
    private long version;

    protected OnboardingApplicationInvitation() {
    }

    public OnboardingApplicationInvitation(OnboardingLead lead, String tokenHash, String applicantSubject,
            OffsetDateTime expiresAt) {
        this.lead = lead;
        this.tokenHash = tokenHash;
        this.applicantSubject = applicantSubject;
        this.expiresAt = expiresAt;
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

    public boolean isExpired() {
        return !expiresAt.isAfter(OffsetDateTime.now());
    }

    public boolean isAccepted() {
        return acceptedAt != null;
    }

    public void accept(UUID applicationId) {
        if (isAccepted()) {
            throw new IllegalStateException("Invitation has already been accepted");
        }
        if (isExpired()) {
            throw new IllegalStateException("Invitation has expired");
        }
        this.applicationId = applicationId;
        this.acceptedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public OnboardingLead getLead() {
        return lead;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public String getApplicantSubject() {
        return applicantSubject;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public OffsetDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
