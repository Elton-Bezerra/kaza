package com.br.bz.kaza.kaza.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "onboarding_applications")
public class OnboardingApplication {
    @Id
    @GeneratedValue
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "applicant_user_id", nullable = false)
    private User applicant;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    private OnboardingLead lead;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private OnboardingApplicationStatus status = OnboardingApplicationStatus.DRAFT;
    @Column(name = "responsible_name", length = 150)
    private String responsibleName;
    @Column(name = "responsible_email")
    private String responsibleEmail;
    @Column(name = "responsible_phone", length = 32)
    private String responsiblePhone;
    @Column(name = "tax_id", length = 14)
    private String taxId;
    @Column(name = "condominium_name")
    private String condominiumName;
    @Column(name = "address_line")
    private String addressLine;
    @Column(name = "address_city", length = 120)
    private String addressCity;
    @Column(name = "address_state", length = 2)
    private String addressState;
    @Column(name = "postal_code", length = 8)
    private String postalCode;
    @Column(name = "proposed_unit_count")
    private Integer proposedUnitCount;
    @Column(name = "subscription_price_per_unit", precision = 10, scale = 2)
    private BigDecimal subscriptionPricePerUnit;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_user_id")
    private User reviewer;
    @Column(name = "review_reason", length = 2000)
    private String reviewReason;
    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;
    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;
    @Column(nullable = false)
    private OffsetDateTime createdAt;
    @Column(nullable = false)
    private OffsetDateTime updatedAt;
    @Version
    private long version;
    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<OnboardingApplicationUnit> units = new ArrayList<>();

    protected OnboardingApplication() {
    }

    public OnboardingApplication(User applicant, OnboardingLead lead) {
        this.applicant = applicant;
        this.lead = lead;
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

    public void update(String responsibleName, String responsibleEmail, String responsiblePhone,
            String taxId, String condominiumName, String addressLine, String addressCity,
            String addressState, String postalCode, Integer proposedUnitCount,
            BigDecimal subscriptionPricePerUnit, List<OnboardingApplicationUnit> replacementUnits) {
        this.responsibleName = responsibleName;
        this.responsibleEmail = responsibleEmail;
        this.responsiblePhone = responsiblePhone;
        this.taxId = taxId;
        this.condominiumName = condominiumName;
        this.addressLine = addressLine;
        this.addressCity = addressCity;
        this.addressState = addressState;
        this.postalCode = postalCode;
        this.proposedUnitCount = proposedUnitCount;
        this.subscriptionPricePerUnit = subscriptionPricePerUnit;
        if (replacementUnits != null) {
            units.clear();
            units.addAll(replacementUnits);
        }
    }

    public void submit() {
        if (status != OnboardingApplicationStatus.DRAFT
                && status != OnboardingApplicationStatus.NEEDS_MORE_INFORMATION) {
            throw new IllegalStateException("Only draft applications or applications needing more information can be submitted");
        }
        status = OnboardingApplicationStatus.UNDER_REVIEW;
        submittedAt = OffsetDateTime.now();
        reviewReason = null;
    }

    public boolean isEditable() {
        return status == OnboardingApplicationStatus.DRAFT
                || status == OnboardingApplicationStatus.NEEDS_MORE_INFORMATION;
    }

    public UUID getId() {
        return id;
    }

    public User getApplicant() {
        return applicant;
    }

    public OnboardingLead getLead() {
        return lead;
    }

    public OnboardingApplicationStatus getStatus() {
        return status;
    }

    public String getResponsibleName() {
        return responsibleName;
    }

    public String getResponsibleEmail() {
        return responsibleEmail;
    }

    public String getResponsiblePhone() {
        return responsiblePhone;
    }

    public String getTaxId() {
        return taxId;
    }

    public String getCondominiumName() {
        return condominiumName;
    }

    public String getAddressLine() {
        return addressLine;
    }

    public String getAddressCity() {
        return addressCity;
    }

    public String getAddressState() {
        return addressState;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public Integer getProposedUnitCount() {
        return proposedUnitCount;
    }

    public BigDecimal getSubscriptionPricePerUnit() {
        return subscriptionPricePerUnit;
    }

    public String getReviewReason() {
        return reviewReason;
    }

    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }

    public OffsetDateTime getReviewedAt() {
        return reviewedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public User getReviewer() {
        return reviewer;
    }

    public long getVersion() {
        return version;
    }

    public List<OnboardingApplicationUnit> getUnits() {
        return units;
    }
}
