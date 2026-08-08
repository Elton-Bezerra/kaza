package com.br.bz.kaza.kaza.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class AdminDtos {
    private AdminDtos() {
    }

    public record LeadSummaryResponse(
            UUID id,
            String name,
            String email,
            String phone,
            String declaredRole,
            boolean contactConsent,
            boolean marketingConsent,
            boolean analyticsConsent,
            String source,
            String landingPath,
            String referrer,
            String utmSource,
            String utmMedium,
            String utmCampaign,
            String utmTerm,
            String utmContent,
            OffsetDateTime createdAt) {
    }

    public record LeadEmailResponse(
            UUID id,
            String type,
            String recipient,
            String status,
            int attempts,
            String lastError,
            OffsetDateTime sentAt,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
    }

    public record LeadInvitationResponse(
            UUID id,
            String status,
            UUID applicationId,
            OffsetDateTime expiresAt,
            OffsetDateTime acceptedAt,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
    }

    public record LeadResponse(
            UUID id,
            String name,
            String email,
            String phone,
            String declaredRole,
            boolean contactConsent,
            boolean marketingConsent,
            boolean analyticsConsent,
            String source,
            String landingPath,
            String utmSource,
            String utmMedium,
            String utmCampaign,
            String utmTerm,
            String utmContent,
            String referrer,
            OffsetDateTime createdAt,
            List<LeadEmailResponse> deliveries,
            List<LeadInvitationResponse> invitations) {
    }

    public record ApplicationSummaryResponse(
            UUID id,
            String status,
            String applicantSubject,
            String applicantEmail,
            String responsibleName,
            String condominiumName,
            Integer proposedUnitCount,
            BigDecimal subscriptionPricePerUnit,
            UUID leadId,
            String leadEmail,
            OffsetDateTime submittedAt,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
    }

    public record ApplicationUnitResponse(String identifier, BigDecimal idealFraction) {
    }

    public record ApplicationResponse(
            UUID id,
            String status,
            String applicantSubject,
            String applicantEmail,
            UUID leadId,
            String leadEmail,
            String responsibleName,
            String responsibleEmail,
            String responsiblePhone,
            String taxId,
            String condominiumName,
            String addressLine,
            String addressCity,
            String addressState,
            String postalCode,
            Integer proposedUnitCount,
            BigDecimal subscriptionPricePerUnit,
            List<ApplicationUnitResponse> units,
            String reviewReason,
            UUID reviewerId,
            String reviewerSubject,
            OffsetDateTime submittedAt,
            OffsetDateTime reviewedAt,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            long version) {
    }

    public record AdminCount(String status, long count) {
    }

    public record AdminSummaryResponse(
            long totalLeads,
            long newLeads,
            long totalApplications,
            long pendingApplications,
            List<AdminCount> leadsByStatus,
            List<AdminCount> applicationsByStatus,
            List<AdminCount> leadSources,
            List<AdminCount> applicationSources,
            OffsetDateTime generatedAt) {
    }
}
