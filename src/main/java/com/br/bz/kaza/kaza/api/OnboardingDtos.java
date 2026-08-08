package com.br.bz.kaza.kaza.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class OnboardingDtos {
    private OnboardingDtos() {
    }

    public record LeadRequest(
            @NotBlank @Size(max = 150) String name,
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Pattern(regexp = "(?=(?:\\D*\\d){8,})[0-9+()\\- .]{8,32}") String phone,
            @NotNull DeclaredRole role,
            @NotNull @AssertTrue Boolean contactConsent,
            boolean marketingConsent,
            boolean analyticsConsent,
            @Size(max = 100) String source,
            @Valid Attribution attribution) {
    }

    public record Attribution(
            @Size(max = 500) String landingPath,
            @Size(max = 2048) String referrer,
            @Size(max = 100) String utmSource,
            @Size(max = 100) String utmMedium,
            @Size(max = 150) String utmCampaign,
            @Size(max = 150) String utmContent,
            @Size(max = 150) String utmTerm) {
    }

    public enum DeclaredRole {
        SINDICO, MORADOR, OUTRO
    }

    public record LeadResponse(UUID id, String status) {
    }

    public record ApplicationInvitationResponse(
            UUID id,
            UUID leadId,
            String leadName,
            String leadEmail,
            String status,
            OffsetDateTime expiresAt,
            OffsetDateTime acceptedAt,
            UUID applicationId) {
    }

    public record LandingEventRequest(
            @NotBlank @Pattern(regexp = "page_view|cta_view|cta_click|form_start|field_complete|submit_success|submit_error")
            String name,
            @NotBlank @Size(max = 500) String pagePath,
            @NotBlank @Size(max = 255) String pageTitle,
            @NotNull OffsetDateTime occurredAt,
            @Valid Attribution attribution,
            @Size(max = 100) String location,
            @Size(max = 100) String field,
            @Min(0) @Max(599) Integer statusCode) {
    }

    public record LandingEventResponse(UUID id, String status) {
    }

    public record CreateApplicationRequest(UUID leadId) {
    }

    public record UnitDraftRequest(
            @NotBlank @Size(max = 100) String identifier,
            @NotNull @DecimalMin(value = "0.00000001") @DecimalMax("1.00000000")
            @Digits(integer = 1, fraction = 8) BigDecimal idealFraction) {
    }

    public record UpdateApplicationRequest(
            @Size(max = 150) String responsibleName,
            @Email @Size(max = 255) String responsibleEmail,
            @Pattern(regexp = "(?=(?:\\D*\\d){8,})[0-9+()\\- .]{8,32}") String responsiblePhone,
            @Pattern(regexp = "[0-9.\\-/]{11,18}") String taxId,
            @Size(max = 255) String condominiumName,
            @Size(max = 255) String addressLine,
            @Size(max = 120) String addressCity,
            @Pattern(regexp = "[A-Za-z]{2}") String addressState,
            @Pattern(regexp = "\\d{8}") String postalCode,
            @Min(1) @Max(500) Integer proposedUnitCount,
            @DecimalMin(value = "0.01") @Digits(integer = 8, fraction = 2)
            BigDecimal subscriptionPricePerUnit,
            @Valid @Size(max = 500) List<UnitDraftRequest> units) {
    }

    public record UnitDraftResponse(String identifier, BigDecimal idealFraction) {
    }

    public record ApplicationResponse(
            UUID id,
            String status,
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
            List<UnitDraftResponse> units,
            String reviewReason,
            OffsetDateTime submittedAt,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            long version) {
    }

    public record DocumentResponse(
            UUID id,
            String filename,
            String contentType,
            long sizeBytes,
            String sha256,
            String scanStatus,
            OffsetDateTime uploadedAt) {
    }

    public record MembershipResponse(UUID condominiumId, String condominiumName, String role) {
    }

    public record OnboardingSummary(UUID applicationId, String status, String reviewReason) {
    }

    public record MeResponse(
            UUID id,
            String subject,
            String email,
            String displayName,
            List<MembershipResponse> memberships,
            OnboardingSummary onboarding) {
    }
}
