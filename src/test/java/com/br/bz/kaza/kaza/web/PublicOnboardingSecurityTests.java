package com.br.bz.kaza.kaza.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.br.bz.kaza.kaza.domain.LandingEvent;
import com.br.bz.kaza.kaza.domain.OnboardingApplication;
import com.br.bz.kaza.kaza.domain.OnboardingLead;
import com.br.bz.kaza.kaza.security.SecurityConfig;
import com.br.bz.kaza.kaza.service.OnboardingApplicationInvitationService;
import com.br.bz.kaza.kaza.service.OnboardingApplicationService;
import com.br.bz.kaza.kaza.service.OnboardingDocumentService;
import com.br.bz.kaza.kaza.service.OnboardingLeadService;
import com.br.bz.kaza.kaza.service.email.LeadEmailOutboxService;
import com.br.bz.kaza.kaza.repository.LandingEventRepository;
import com.br.bz.kaza.kaza.service.PublicOnboardingSession;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({OnboardingLeadController.class, LandingEventController.class, WebhookController.class,
        PublicOnboardingController.class})
@Import(SecurityConfig.class)
@TestPropertySource(properties = "asaas.webhook-token=test-secret")
class PublicOnboardingSecurityTests {
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private OnboardingLeadService leads;
    @MockitoBean
    private LeadEmailOutboxService emails;
    @MockitoBean
    private LandingEventRepository events;
    @MockitoBean
    private OnboardingApplicationInvitationService invitations;
    @MockitoBean
    private OnboardingApplicationService applications;
    @MockitoBean
    private OnboardingDocumentService documents;
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void publicLeadAndLandingEventEndpointsAllowAnonymousRequests() throws Exception {
        OnboardingLead lead = mock(OnboardingLead.class);
        when(lead.getId()).thenReturn(UUID.fromString("33333333-3333-3333-3333-333333333333"));
        when(leads.capture(any())).thenReturn(lead);
        doNothing().when(emails).dispatchPendingForLead(any());

        LandingEvent event = mock(LandingEvent.class);
        when(event.getId()).thenReturn(UUID.fromString("44444444-4444-4444-4444-444444444444"));
        when(events.save(any())).thenReturn(event);

        mvc.perform(post("/api/v1/onboarding/leads")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Maria da Silva",
                                  "email": "maria@example.com",
                                  "phone": "+55 11 99999-9999",
                                  "role": "SINDICO",
                                  "contactConsent": true,
                                  "marketingConsent": false,
                                  "analyticsConsent": false,
                                  "source": "web"
                                }
                                """))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/public/landing-events")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "page_view",
                                  "pagePath": "/",
                                  "pageTitle": "Home",
                                  "occurredAt": "2026-08-08T18:09:10Z"
                                }
                                """))
                .andExpect(status().isAccepted());
    }

    @Test
    void publicInvitationAndApplicationEndpointsAllowAnonymousRequests() throws Exception {
        OnboardingApplication application = mock(OnboardingApplication.class);
        when(application.getId()).thenReturn(UUID.fromString("55555555-5555-5555-5555-555555555555"));
        when(application.getStatus()).thenReturn(com.br.bz.kaza.kaza.domain.OnboardingApplicationStatus.DRAFT);
        when(application.getUnits()).thenReturn(java.util.List.of());
        when(application.getCreatedAt()).thenReturn(OffsetDateTime.parse("2026-08-08T18:09:10Z"));
        when(application.getUpdatedAt()).thenReturn(OffsetDateTime.parse("2026-08-08T18:09:10Z"));
        when(application.getVersion()).thenReturn(1L);

        PublicOnboardingSession onboardingSession = new PublicOnboardingSession(
                                UUID.fromString("66666666-6666-6666-6666-666666666666"),
                                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                                application.getId(),
                                "onboarding-invitation:33333333-3333-3333-3333-333333333333",
                                "maria@example.com",
                                "Maria da Silva",
                                java.time.Instant.now());
        when(invitations.viewInvitation(any())).thenReturn(new com.br.bz.kaza.kaza.service.OnboardingApplicationInvitationService.InvitationView(
                                UUID.fromString("66666666-6666-6666-6666-666666666666"),
                                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                                "Maria da Silva",
                                "maria@example.com",
                                "PENDING",
                                OffsetDateTime.parse("2026-08-15T18:09:10Z"),
                                null,
                                null));
        when(invitations.acceptInvitation(any())).thenReturn(
                new OnboardingApplicationInvitationService.InvitationAcceptance(onboardingSession, application));
        when(applications.getOwned(any(), any())).thenReturn(application);
        when(applications.update(any(), any(), any())).thenReturn(application);
        when(applications.submit(any(), any())).thenReturn(application);
        when(documents.list(any(), any())).thenReturn(java.util.List.of());

        MockHttpSession mockSession = new MockHttpSession();
        mvc.perform(post("/api/v1/public/onboarding/invitations/{token}/accept", "invite-token")
                        .session(mockSession))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/public/onboarding/application/submit")
                        .session(mockSession))
                .andExpect(status().isOk());
    }

    @Test
    void webhookEndpointRejectsAnonymousRequestsWithoutTheSharedSecret() throws Exception {
        mvc.perform(post("/api/v1/webhooks/asaas")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
