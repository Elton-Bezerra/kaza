package com.br.bz.kaza.kaza.service;

import com.br.bz.kaza.kaza.api.OnboardingDtos;
import com.br.bz.kaza.kaza.domain.OnboardingLead;
import com.br.bz.kaza.kaza.repository.OnboardingLeadRepository;
import com.br.bz.kaza.kaza.service.email.LeadEmailOutboxService;
import com.br.bz.kaza.kaza.web.TooManyRequestsException;
import java.time.OffsetDateTime;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OnboardingLeadService {
    private static final int COOLDOWN_MINUTES = 15;
    private final OnboardingLeadRepository leads;
    private final LeadEmailOutboxService emails;

    public OnboardingLeadService(OnboardingLeadRepository leads, LeadEmailOutboxService emails) {
        this.leads = leads;
        this.emails = emails;
    }

    @Transactional
    public OnboardingLead capture(OnboardingDtos.LeadRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (leads.findFirstByEmailIgnoreCaseAndCreatedAtAfterOrderByCreatedAtDesc(
                email, OffsetDateTime.now().minusMinutes(COOLDOWN_MINUTES)).isPresent()) {
            throw new TooManyRequestsException("Please wait before submitting another onboarding request");
        }
        OnboardingDtos.Attribution attribution = request.analyticsConsent()
                ? request.attribution() : null;
        OnboardingLead lead = leads.save(new OnboardingLead(
                clean(request.name()),
                email,
                clean(request.phone()),
                request.role().name(),
                request.contactConsent(),
                request.marketingConsent(),
                request.analyticsConsent(),
                optional(request.source()),
                attribution == null ? null : optional(attribution.landingPath()),
                attribution == null ? null : optional(attribution.utmSource()),
                attribution == null ? null : optional(attribution.utmMedium()),
                attribution == null ? null : optional(attribution.utmCampaign()),
                attribution == null ? null : optional(attribution.utmTerm()),
                attribution == null ? null : optional(attribution.utmContent()),
                attribution == null ? null : optional(attribution.referrer())));
        emails.queueNotifications(lead);
        return lead;
    }

    private String clean(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    private String optional(String value) {
        return value == null || value.isBlank()
                ? null
                : value.replaceAll("\\p{Cntrl}", "").trim();
    }
}
