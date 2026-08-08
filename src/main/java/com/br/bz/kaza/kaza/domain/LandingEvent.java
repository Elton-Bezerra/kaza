package com.br.bz.kaza.kaza.domain;

import com.br.bz.kaza.kaza.api.OnboardingDtos;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "landing_events")
public class LandingEvent {
    @Id
    @GeneratedValue
    private UUID id;
    @Column(name = "event_name", nullable = false, length = 32)
    private String eventName;
    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;
    @Column(name = "received_at", nullable = false)
    private OffsetDateTime receivedAt;
    @Column(name = "page_path", nullable = false, length = 500)
    private String pagePath;
    @Column(name = "page_title", nullable = false, length = 255)
    private String pageTitle;
    @Column(length = 100)
    private String location;
    @Column(name = "field_name", length = 100)
    private String fieldName;
    @Column(name = "status_code")
    private Integer statusCode;
    @Column(name = "landing_path", length = 500)
    private String landingPath;
    @Column(length = 2048)
    private String referrer;
    @Column(name = "utm_source", length = 100)
    private String utmSource;
    @Column(name = "utm_medium", length = 100)
    private String utmMedium;
    @Column(name = "utm_campaign", length = 150)
    private String utmCampaign;
    @Column(name = "utm_content", length = 150)
    private String utmContent;
    @Column(name = "utm_term", length = 150)
    private String utmTerm;

    protected LandingEvent() {
    }

    public LandingEvent(OnboardingDtos.LandingEventRequest request) {
        this.eventName = request.name();
        this.occurredAt = request.occurredAt();
        this.receivedAt = OffsetDateTime.now();
        this.pagePath = request.pagePath();
        this.pageTitle = request.pageTitle();
        this.location = request.location();
        this.fieldName = request.field();
        this.statusCode = request.statusCode();
        OnboardingDtos.Attribution attribution = request.attribution();
        if (attribution != null) {
            this.landingPath = attribution.landingPath();
            this.referrer = attribution.referrer();
            this.utmSource = attribution.utmSource();
            this.utmMedium = attribution.utmMedium();
            this.utmCampaign = attribution.utmCampaign();
            this.utmContent = attribution.utmContent();
            this.utmTerm = attribution.utmTerm();
        }
    }

    public UUID getId() {
        return id;
    }
}
