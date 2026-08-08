package com.br.bz.kaza.kaza.service;

import com.br.bz.kaza.kaza.api.AdminDtos;
import com.br.bz.kaza.kaza.domain.LeadEmailDelivery;
import com.br.bz.kaza.kaza.domain.OnboardingApplicationInvitation;
import com.br.bz.kaza.kaza.domain.OnboardingApplication;
import com.br.bz.kaza.kaza.domain.OnboardingApplicationStatus;
import com.br.bz.kaza.kaza.domain.OnboardingLead;
import com.br.bz.kaza.kaza.repository.LeadEmailDeliveryRepository;
import com.br.bz.kaza.kaza.repository.OnboardingApplicationInvitationRepository;
import com.br.bz.kaza.kaza.repository.OnboardingApplicationRepository;
import com.br.bz.kaza.kaza.repository.OnboardingLeadRepository;
import com.br.bz.kaza.kaza.service.OnboardingApplicationInvitationService.InvitationResult;
import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminOnboardingService {
    private final OnboardingLeadRepository leads;
    private final OnboardingApplicationRepository applications;
    private final LeadEmailDeliveryRepository deliveries;
    private final OnboardingApplicationInvitationRepository invitations;
    private final OnboardingApplicationInvitationService invitationService;

    public AdminOnboardingService(OnboardingLeadRepository leads,
            OnboardingApplicationRepository applications,
            LeadEmailDeliveryRepository deliveries,
            OnboardingApplicationInvitationRepository invitations,
            OnboardingApplicationInvitationService invitationService) {
        this.leads = leads;
        this.applications = applications;
        this.deliveries = deliveries;
        this.invitations = invitations;
        this.invitationService = invitationService;
    }

    @Transactional(readOnly = true)
    public Page<AdminDtos.LeadSummaryResponse> listLeads(String query, Pageable pageable) {
        Page<OnboardingLead> page = query == null || query.isBlank()
                ? leads.findAllByOrderByCreatedAtDesc(pageable)
                : leads.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByCreatedAtDesc(
                        query, query, pageable);
        return page.map(this::leadSummary);
    }

    @Transactional(readOnly = true)
    public AdminDtos.LeadResponse getLead(UUID id) {
        OnboardingLead lead = leads.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Onboarding lead not found"));
        return leadResponse(lead);
    }

    @Transactional(readOnly = true)
    public Page<AdminDtos.ApplicationSummaryResponse> listApplications(
            OnboardingApplicationStatus status, Pageable pageable) {
        Page<OnboardingApplication> page = status == null
                ? applications.findAllByOrderByCreatedAtDesc(pageable)
                : applications.findByStatusOrderByCreatedAtDesc(status, pageable);
        return page.map(this::applicationSummary);
    }

    @Transactional(readOnly = true)
    public AdminDtos.ApplicationResponse getApplication(UUID id) {
        OnboardingApplication application = applications.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Onboarding application not found"));
        application.getUnits().size();
        return applicationResponse(application);
    }

    @Transactional
    public AdminDtos.LeadInvitationResponse inviteLead(UUID id) {
        InvitationResult result = invitationService.inviteLead(id);
        return invitationResponse(result.invitation());
    }

    @Transactional(readOnly = true)
    public AdminDtos.AdminSummaryResponse summary() {
        var leadRecords = leads.findAll();
        var applicationRecords = applications.findAll();
        var applicationsByStatus = applicationRecords.stream()
                .collect(Collectors.groupingBy(application -> application.getStatus().name(), Collectors.counting()));
        var leadSources = leadRecords.stream()
                .map(lead -> lead.getSource() == null || lead.getSource().isBlank() ? "unknown" : lead.getSource())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        var applicationSources = applicationRecords.stream()
                .map(application -> application.getLead() == null || application.getLead().getSource() == null
                        || application.getLead().getSource().isBlank() ? "unknown" : application.getLead().getSource())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        long pendingApplications = applicationRecords.stream()
                .filter(application -> application.getStatus() == OnboardingApplicationStatus.UNDER_REVIEW
                        || application.getStatus() == OnboardingApplicationStatus.NEEDS_MORE_INFORMATION)
                .count();
        return new AdminDtos.AdminSummaryResponse(
                leadRecords.size(),
                leadRecords.size(),
                applicationRecords.size(),
                pendingApplications,
                List.of(),
                toCounts(applicationsByStatus),
                toCounts(leadSources),
                toCounts(applicationSources),
                java.time.OffsetDateTime.now());
    }

    private List<AdminDtos.AdminCount> toCounts(Map<String, Long> counts) {
        return counts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new AdminDtos.AdminCount(entry.getKey(), entry.getValue()))
                .toList();
    }

    private AdminDtos.LeadSummaryResponse leadSummary(OnboardingLead lead) {
        return new AdminDtos.LeadSummaryResponse(
                lead.getId(),
                lead.getName(),
                lead.getEmail(),
                lead.getPhone(),
                lead.getDeclaredRole(),
                lead.isContactConsent(),
                lead.isMarketingConsent(),
                lead.isAnalyticsConsent(),
                lead.getSource(),
                lead.getLandingPath(),
                lead.getReferrer(),
                lead.getUtmSource(),
                lead.getUtmMedium(),
                lead.getUtmCampaign(),
                lead.getUtmTerm(),
                lead.getUtmContent(),
                lead.getCreatedAt());
    }

    private AdminDtos.LeadResponse leadResponse(OnboardingLead lead) {
        return new AdminDtos.LeadResponse(
                lead.getId(),
                lead.getName(),
                lead.getEmail(),
                lead.getPhone(),
                lead.getDeclaredRole(),
                lead.isContactConsent(),
                lead.isMarketingConsent(),
                lead.isAnalyticsConsent(),
                lead.getSource(),
                lead.getLandingPath(),
                lead.getUtmSource(),
                lead.getUtmMedium(),
                lead.getUtmCampaign(),
                lead.getUtmTerm(),
                lead.getUtmContent(),
                lead.getReferrer(),
                lead.getCreatedAt(),
                deliveries.findByLeadIdOrderByCreatedAtAsc(lead.getId()).stream()
                        .map(this::deliveryResponse)
                        .toList(),
                invitations.findByLeadIdOrderByCreatedAtDesc(lead.getId()).stream()
                        .map(this::invitationResponse)
                        .toList());
    }

    private AdminDtos.LeadEmailResponse deliveryResponse(LeadEmailDelivery delivery) {
        return new AdminDtos.LeadEmailResponse(
                delivery.getId(),
                delivery.getType().name(),
                delivery.getRecipient(),
                delivery.getStatus().name(),
                delivery.getAttempts(),
                delivery.getLastError(),
                delivery.getSentAt(),
                delivery.getCreatedAt(),
                delivery.getUpdatedAt());
    }

    private AdminDtos.ApplicationSummaryResponse applicationSummary(OnboardingApplication application) {
        return new AdminDtos.ApplicationSummaryResponse(
                application.getId(),
                application.getStatus().name(),
                application.getApplicant().getSubject(),
                application.getApplicant().getEmail(),
                application.getResponsibleName(),
                application.getCondominiumName(),
                application.getProposedUnitCount(),
                application.getSubscriptionPricePerUnit(),
                application.getLead() == null ? null : application.getLead().getId(),
                application.getLead() == null ? null : application.getLead().getEmail(),
                application.getSubmittedAt(),
                application.getCreatedAt(),
                application.getUpdatedAt());
    }

    private AdminDtos.ApplicationResponse applicationResponse(OnboardingApplication application) {
        return new AdminDtos.ApplicationResponse(
                application.getId(),
                application.getStatus().name(),
                application.getApplicant().getSubject(),
                application.getApplicant().getEmail(),
                application.getLead() == null ? null : application.getLead().getId(),
                application.getLead() == null ? null : application.getLead().getEmail(),
                application.getResponsibleName(),
                application.getResponsibleEmail(),
                application.getResponsiblePhone(),
                application.getTaxId(),
                application.getCondominiumName(),
                application.getAddressLine(),
                application.getAddressCity(),
                application.getAddressState(),
                application.getPostalCode(),
                application.getProposedUnitCount(),
                application.getSubscriptionPricePerUnit(),
                application.getUnits().stream()
                        .map(unit -> new AdminDtos.ApplicationUnitResponse(
                                unit.getIdentifier(), unit.getIdealFraction()))
                        .toList(),
                application.getReviewReason(),
                application.getReviewer() == null ? null : application.getReviewer().getId(),
                application.getReviewer() == null ? null : application.getReviewer().getSubject(),
                application.getSubmittedAt(),
                application.getReviewedAt(),
                application.getCreatedAt(),
                application.getUpdatedAt(),
                application.getVersion());
    }

    private AdminDtos.LeadInvitationResponse invitationResponse(OnboardingApplicationInvitation invitation) {
        String status = invitation.isAccepted() ? "ACCEPTED" : invitation.isExpired() ? "EXPIRED" : "PENDING";
        return new AdminDtos.LeadInvitationResponse(
                invitation.getId(),
                status,
                invitation.getApplicationId(),
                invitation.getExpiresAt(),
                invitation.getAcceptedAt(),
                invitation.getCreatedAt(),
                invitation.getUpdatedAt());
    }
}
