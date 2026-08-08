package com.br.bz.kaza.kaza.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "onboarding_leads")
public class OnboardingLead {
    @Id
    @GeneratedValue
    private UUID id;
    @Column(nullable = false, length = 150)
    private String name;
    @Column(nullable = false)
    private String email;
    @Column(nullable = false, length = 32)
    private String phone;
    @Column(name = "declared_role", nullable = false, length = 32)
    private String declaredRole;
    @Column(name = "contact_consent", nullable = false)
    private boolean contactConsent;
    @Column(name = "marketing_consent", nullable = false)
    private boolean marketingConsent;
    @Column(name = "analytics_consent", nullable = false)
    private boolean analyticsConsent;
    @Column(length = 100)
    private String source;
    @Column(name = "landing_path", length = 500)
    private String landingPath;
    @Column(name = "utm_source", length = 100)
    private String utmSource;
    @Column(name = "utm_medium", length = 100)
    private String utmMedium;
    @Column(name = "utm_campaign", length = 150)
    private String utmCampaign;
    @Column(name = "utm_term", length = 150)
    private String utmTerm;
    @Column(name = "utm_content", length = 150)
    private String utmContent;
    @Column(length = 2048)
    private String referrer;
    @Column(nullable = false)
    private OffsetDateTime createdAt;

    protected OnboardingLead() {
    }

    public OnboardingLead(String name, String email, String phone, String declaredRole,
            boolean contactConsent, boolean marketingConsent, boolean analyticsConsent,
            String source, String landingPath, String utmSource, String utmMedium,
            String utmCampaign, String utmTerm, String utmContent, String referrer) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.declaredRole = declaredRole;
        this.contactConsent = contactConsent;
        this.marketingConsent = marketingConsent;
        this.analyticsConsent = analyticsConsent;
        this.source = source;
        this.landingPath = landingPath;
        this.utmSource = utmSource;
        this.utmMedium = utmMedium;
        this.utmCampaign = utmCampaign;
        this.utmTerm = utmTerm;
        this.utmContent = utmContent;
        this.referrer = referrer;
    }

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getDeclaredRole() {
        return declaredRole;
    }

    public boolean isContactConsent() {
        return contactConsent;
    }

    public boolean isMarketingConsent() {
        return marketingConsent;
    }

    public boolean isAnalyticsConsent() {
        return analyticsConsent;
    }

    public String getSource() {
        return source;
    }

    public String getLandingPath() {
        return landingPath;
    }

    public String getUtmSource() {
        return utmSource;
    }

    public String getUtmMedium() {
        return utmMedium;
    }

    public String getUtmCampaign() {
        return utmCampaign;
    }

    public String getUtmTerm() {
        return utmTerm;
    }

    public String getUtmContent() {
        return utmContent;
    }

    public String getReferrer() {
        return referrer;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
