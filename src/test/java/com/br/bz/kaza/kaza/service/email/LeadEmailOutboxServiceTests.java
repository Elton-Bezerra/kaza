package com.br.bz.kaza.kaza.service.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.br.bz.kaza.kaza.domain.LeadEmailDelivery;
import com.br.bz.kaza.kaza.domain.LeadEmailDeliveryStatus;
import com.br.bz.kaza.kaza.domain.LeadEmailDeliveryType;
import com.br.bz.kaza.kaza.domain.OnboardingLead;
import com.br.bz.kaza.kaza.repository.LeadEmailDeliveryRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class LeadEmailOutboxServiceTests {
    private LeadEmailDeliveryRepository deliveries;
    private EmailProvider provider;
    private LeadEmailOutboxService service;
    private OnboardingLead lead;

    @BeforeEach
    void setUp() {
        deliveries = mock(LeadEmailDeliveryRepository.class);
        provider = mock(EmailProvider.class);
        service = new LeadEmailOutboxService(deliveries, provider,
                new KazaMailProperties(
                        "no-reply@kaza.local",
                        new KazaMailProperties.Leads(
                                "super-admin@kaza.local",
                                "Lead {{name}}",
                                "Internal {{email}}",
                                "Hello {{name}}",
                                "Prospect {{email}}",
                                "Invite {{name}}",
                                "Invitation {{invitationUrl}} {{expiresAt}}")));
        lead = new OnboardingLead(
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
        ReflectionTestUtils.setField(lead, "createdAt", OffsetDateTime.parse("2026-08-08T12:00:00Z"));
    }

    @Test
    void queueNotificationsCreatesInternalAndProspectMessages() {
        service.queueNotifications(lead);

        @SuppressWarnings("unchecked")
        var captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(deliveries).saveAll(captor.capture());
        List<LeadEmailDelivery> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).getType()).isEqualTo(LeadEmailDeliveryType.INTERNAL_NOTIFICATION);
        assertThat(saved.get(0).getRecipient()).isEqualTo("super-admin@kaza.local");
        assertThat(saved.get(1).getType()).isEqualTo(LeadEmailDeliveryType.PROSPECT_CONFIRMATION);
        assertThat(saved.get(1).getRecipient()).isEqualTo("maria@example.com");
    }

    @Test
    void queueInvitationCreatesSingleInvitationMessage() {
        service.queueInvitation(lead, "http://localhost:3000/invite/token", OffsetDateTime.parse("2026-08-15T12:00:00Z"));

        @SuppressWarnings("unchecked")
        var captor = org.mockito.ArgumentCaptor.forClass(LeadEmailDelivery.class);
        verify(deliveries).save(captor.capture());
        LeadEmailDelivery saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo(LeadEmailDeliveryType.INVITATION);
        assertThat(saved.getRecipient()).isEqualTo("maria@example.com");
        assertThat(saved.getBody()).contains("http://localhost:3000/invite/token");
    }

    @Test
    void dispatchPendingMarksMessagesAsSent() {
        LeadEmailDelivery internal = new LeadEmailDelivery(
                lead.getId(), LeadEmailDeliveryType.INTERNAL_NOTIFICATION, "no-reply@kaza.local",
                "super-admin@kaza.local", "Lead Maria da Silva", "Internal maria@example.com");
        LeadEmailDelivery prospect = new LeadEmailDelivery(
                lead.getId(), LeadEmailDeliveryType.PROSPECT_CONFIRMATION, "no-reply@kaza.local",
                "maria@example.com", "Hello Maria da Silva", "Prospect maria@example.com");
        when(deliveries.findByLeadIdAndStatusOrderByCreatedAtAsc(lead.getId(), LeadEmailDeliveryStatus.PENDING))
                .thenReturn(List.of(internal, prospect));
        when(deliveries.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.dispatchPendingForLead(lead.getId());

        verify(provider, times(2)).send(any());
        assertThat(internal.getStatus()).isEqualTo(LeadEmailDeliveryStatus.SENT);
        assertThat(internal.getAttempts()).isEqualTo(1);
        assertThat(prospect.getStatus()).isEqualTo(LeadEmailDeliveryStatus.SENT);
    }

    @Test
    void dispatchPendingRecordsFailuresAndStillProcessesOtherMessages() {
        LeadEmailDelivery internal = new LeadEmailDelivery(
                lead.getId(), LeadEmailDeliveryType.INTERNAL_NOTIFICATION, "no-reply@kaza.local",
                "super-admin@kaza.local", "Lead Maria da Silva", "Internal maria@example.com");
        LeadEmailDelivery prospect = new LeadEmailDelivery(
                lead.getId(), LeadEmailDeliveryType.PROSPECT_CONFIRMATION, "no-reply@kaza.local",
                "maria@example.com", "Hello Maria da Silva", "Prospect maria@example.com");
        when(deliveries.findByLeadIdAndStatusOrderByCreatedAtAsc(lead.getId(), LeadEmailDeliveryStatus.PENDING))
                .thenReturn(List.of(internal, prospect));
        when(deliveries.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doAnswer(invocation -> {
            EmailProvider.EmailMessage message = invocation.getArgument(0);
            if ("maria@example.com".equals(message.to())) {
                throw new IllegalStateException("SMTP down");
            }
            return null;
        }).when(provider).send(any());

        assertThatThrownBy(() -> service.dispatchPendingForLead(lead.getId()))
                .isInstanceOf(EmailDispatchException.class)
                .hasMessageContaining("PROSPECT_CONFIRMATION");
        assertThat(internal.getStatus()).isEqualTo(LeadEmailDeliveryStatus.SENT);
        assertThat(prospect.getStatus()).isEqualTo(LeadEmailDeliveryStatus.FAILED);
        assertThat(prospect.getLastError()).contains("SMTP down");
    }
}
