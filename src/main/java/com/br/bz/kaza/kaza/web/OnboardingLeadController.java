package com.br.bz.kaza.kaza.web;

import com.br.bz.kaza.kaza.api.OnboardingDtos;
import com.br.bz.kaza.kaza.domain.OnboardingLead;
import com.br.bz.kaza.kaza.service.OnboardingLeadService;
import com.br.bz.kaza.kaza.service.email.LeadEmailOutboxService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/onboarding/leads")
public class OnboardingLeadController {
    private final OnboardingLeadService leads;
    private final LeadEmailOutboxService emails;

    public OnboardingLeadController(OnboardingLeadService leads, LeadEmailOutboxService emails) {
        this.leads = leads;
        this.emails = emails;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OnboardingDtos.LeadResponse create(@Valid @RequestBody OnboardingDtos.LeadRequest request) {
        OnboardingLead lead = leads.capture(request);
        emails.dispatchPendingForLead(lead.getId());
        return new OnboardingDtos.LeadResponse(lead.getId(), "RECEIVED");
    }
}
