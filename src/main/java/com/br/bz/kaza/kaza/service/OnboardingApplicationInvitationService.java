package com.br.bz.kaza.kaza.service;

import com.br.bz.kaza.kaza.api.OnboardingDtos;
import com.br.bz.kaza.kaza.domain.LeadEmailDeliveryType;
import com.br.bz.kaza.kaza.domain.OnboardingApplication;
import com.br.bz.kaza.kaza.domain.OnboardingApplicationInvitation;
import com.br.bz.kaza.kaza.domain.OnboardingLead;
import com.br.bz.kaza.kaza.repository.OnboardingApplicationInvitationRepository;
import com.br.bz.kaza.kaza.repository.OnboardingLeadRepository;
import com.br.bz.kaza.kaza.service.email.LeadEmailOutboxService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OnboardingApplicationInvitationService {
    private static final Duration INVITATION_TTL = Duration.ofDays(7);
    private static final String INVITATION_SUBJECT_PREFIX = "onboarding-invitation:";

    private final OnboardingLeadRepository leads;
    private final OnboardingApplicationService applications;
    private final OnboardingApplicationInvitationRepository invitations;
    private final LeadEmailOutboxService emails;
    private final UserService users;
    private final String publicUrl;
    private final SecureRandom secureRandom = new SecureRandom();

    public OnboardingApplicationInvitationService(OnboardingLeadRepository leads,
            OnboardingApplicationService applications,
            OnboardingApplicationInvitationRepository invitations,
            LeadEmailOutboxService emails,
            UserService users,
            @Value("${kaza.web.public-url:http://localhost:3000}") String publicUrl) {
        this.leads = leads;
        this.applications = applications;
        this.invitations = invitations;
        this.emails = emails;
        this.users = users;
        this.publicUrl = normalizeBaseUrl(publicUrl);
    }

    @Transactional
    public InvitationResult inviteLead(UUID leadId) {
        OnboardingLead lead = leads.findById(leadId)
                .orElseThrow(() -> new IllegalArgumentException("Onboarding lead not found"));
        String token = generateToken();
        String tokenHash = hashToken(token);
        OffsetDateTime expiresAt = OffsetDateTime.now().plus(INVITATION_TTL);
        OnboardingApplicationInvitation invitation = invitations.save(new OnboardingApplicationInvitation(
                lead, tokenHash, applicantSubject(leadId), expiresAt));
        String invitationUrl = invitationUrl(token);
        emails.queueInvitation(lead, invitationUrl, expiresAt);
        emails.dispatchPendingForLead(leadId, java.util.Set.of(LeadEmailDeliveryType.INVITATION));
        return new InvitationResult(invitation, invitationUrl, token);
    }

    @Transactional(readOnly = true)
    public InvitationView viewInvitation(String token) {
        OnboardingApplicationInvitation invitation = findInvitation(token);
        return toView(invitation);
    }

    @Transactional
    public InvitationAcceptance acceptInvitation(String token) {
        OnboardingApplicationInvitation invitation = findInvitation(token);
        if (invitation.isExpired()) {
            throw new IllegalStateException("Invitation has expired");
        }
        OnboardingLead lead = invitation.getLead();
        String subject = invitation.getApplicantSubject();
        Jwt jwt = buildJwt(subject, lead.getEmail(), lead.getName());
        OnboardingApplication application;
        if (invitation.isAccepted() && invitation.getApplicationId() != null) {
            application = applications.getById(invitation.getApplicationId());
        } else {
            application = applications.findLatestByApplicantSubject(subject)
                    .orElseGet(() -> applications.create(
                            new OnboardingDtos.CreateApplicationRequest(lead.getId()), jwt));
            invitation.accept(application.getId());
        }
        users.ensureFromIdentity(subject, lead.getEmail(), lead.getName());
        return new InvitationAcceptance(
                new PublicOnboardingSession(
                        invitation.getId(),
                        lead.getId(),
                        application.getId(),
                        subject,
                        lead.getEmail(),
                        lead.getName(),
                        Instant.now()),
                application);
    }

    private InvitationView toView(OnboardingApplicationInvitation invitation) {
        return new InvitationView(
                invitation.getId(),
                invitation.getLead().getId(),
                invitation.getLead().getName(),
                invitation.getLead().getEmail(),
                invitation.isAccepted() ? "ACCEPTED" : invitation.isExpired() ? "EXPIRED" : "PENDING",
                invitation.getExpiresAt(),
                invitation.getAcceptedAt(),
                invitation.getApplicationId());
    }

    private OnboardingApplicationInvitation findInvitation(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Invitation token is required");
        }
        return invitations.findByTokenHash(hashToken(token))
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found"));
    }

    private String invitationUrl(String token) {
        return publicUrl + "/invite/" + token;
    }

    private String normalizeBaseUrl(String value) {
        String trimmed = value == null ? "" : value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed.isBlank() ? "http://localhost:3000" : trimmed;
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String applicantSubject(UUID leadId) {
        return INVITATION_SUBJECT_PREFIX + leadId;
    }

    private Jwt buildJwt(String subject, String email, String name) {
        Instant issuedAt = Instant.now();
        return new Jwt("public-onboarding-token", issuedAt, issuedAt.plusSeconds(60),
                java.util.Map.of("alg", "none"),
                java.util.Map.of("sub", subject, "email", email, "name", name));
    }

    public record InvitationResult(OnboardingApplicationInvitation invitation, String invitationUrl, String token) {
    }

    public record InvitationAcceptance(PublicOnboardingSession session, OnboardingApplication application) {
    }

    public record InvitationView(
            UUID id,
            UUID leadId,
            String leadName,
            String leadEmail,
            String status,
            OffsetDateTime expiresAt,
            OffsetDateTime acceptedAt,
            UUID applicationId) {
    }
}
