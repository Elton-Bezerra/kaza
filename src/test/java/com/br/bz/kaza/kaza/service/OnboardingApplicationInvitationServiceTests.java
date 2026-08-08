package com.br.bz.kaza.kaza.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.br.bz.kaza.kaza.domain.LeadEmailDeliveryType;
import com.br.bz.kaza.kaza.domain.OnboardingApplication;
import com.br.bz.kaza.kaza.domain.OnboardingApplicationInvitation;
import com.br.bz.kaza.kaza.domain.OnboardingLead;
import com.br.bz.kaza.kaza.domain.User;
import com.br.bz.kaza.kaza.repository.OnboardingApplicationInvitationRepository;
import com.br.bz.kaza.kaza.repository.OnboardingLeadRepository;
import com.br.bz.kaza.kaza.service.email.LeadEmailOutboxService;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class OnboardingApplicationInvitationServiceTests {
    private OnboardingLeadRepository leads;
    private OnboardingApplicationService applications;
    private OnboardingApplicationInvitationRepository invitations;
    private LeadEmailOutboxService emails;
    private UserService users;
    private OnboardingApplicationInvitationService service;

    @BeforeEach
    void setUp() {
        leads = mock(OnboardingLeadRepository.class);
        applications = mock(OnboardingApplicationService.class);
        invitations = mock(OnboardingApplicationInvitationRepository.class);
        emails = mock(LeadEmailOutboxService.class);
        users = mock(UserService.class);
        service = new OnboardingApplicationInvitationService(
                leads,
                applications,
                invitations,
                emails,
                users,
                "http://localhost:3000");
    }

    @Test
    void inviteLeadHashesTokenAndQueuesEmail() {
        OnboardingLead lead = lead();
        when(leads.findById(lead.getId())).thenReturn(Optional.of(lead));
        when(invitations.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.inviteLead(lead.getId());

        assertThat(result.token()).isNotBlank();
        assertThat(result.invitation().getTokenHash()).hasSize(64);
        verify(emails).queueInvitation(any(), any(), any());
        verify(emails).dispatchPendingForLead(lead.getId(), java.util.Set.of(LeadEmailDeliveryType.INVITATION));
    }

    @Test
    void acceptInvitationCreatesOrReusesApplication() {
        OnboardingLead lead = lead();
        String token = "invite-token";
        OnboardingApplicationInvitation invitation = new OnboardingApplicationInvitation(
                lead,
                hash(token),
                "onboarding-invitation:" + lead.getId(),
                OffsetDateTime.now().plusDays(7));
        ReflectionTestUtils.setField(invitation, "id", UUID.randomUUID());
        when(invitations.findByTokenHash(hash(token))).thenReturn(Optional.of(invitation));
        when(applications.findLatestByApplicantSubject("onboarding-invitation:" + lead.getId()))
                .thenReturn(Optional.empty());
        OnboardingApplication application = new OnboardingApplication(new User(
                "onboarding-invitation:" + lead.getId(),
                lead.getEmail(),
                lead.getName()), lead);
        ReflectionTestUtils.setField(application, "id", UUID.randomUUID());
        when(applications.create(any(), any())).thenReturn(application);

        OnboardingApplicationInvitationService.InvitationAcceptance acceptance = service.acceptInvitation(token);
        PublicOnboardingSession session = acceptance.session();

        assertThat(session.applicationId()).isEqualTo(application.getId());
        assertThat(invitation.getAcceptedAt()).isNotNull();
        verify(users).ensureFromIdentity("onboarding-invitation:" + lead.getId(), lead.getEmail(), lead.getName());
    }

    private OnboardingLead lead() {
        OnboardingLead lead = new OnboardingLead(
                "Maria da Silva",
                "maria@example.com",
                "+55 11 99999-9999",
                "SINDICO",
                true,
                false,
                true,
                "web-landing",
                "/landing",
                "google",
                "cpc",
                "camp",
                "term",
                "content",
                "https://example.com");
        ReflectionTestUtils.setField(lead, "id", UUID.randomUUID());
        return lead;
    }

    private String hash(String token) {
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
