package com.br.bz.kaza.kaza.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.br.bz.kaza.kaza.api.OnboardingDtos;
import com.br.bz.kaza.kaza.domain.OnboardingLead;
import com.br.bz.kaza.kaza.repository.OnboardingLeadRepository;
import com.br.bz.kaza.kaza.service.email.LeadEmailOutboxService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class OnboardingLeadServiceTests {
    private OnboardingLeadRepository leads;
    private LeadEmailOutboxService emails;
    private OnboardingLeadService service;

    @BeforeEach
    void setUp() {
        leads = mock(OnboardingLeadRepository.class);
        emails = mock(LeadEmailOutboxService.class);
        service = new OnboardingLeadService(leads, emails);
        when(leads.findFirstByEmailIgnoreCaseAndCreatedAtAfterOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.empty());
        when(leads.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void attributionIsDroppedWithoutAnalyticsConsent() {
        service.capture(new OnboardingDtos.LeadRequest(
                "Maria da Silva",
                "Maria@example.com",
                "+55 11 99999-9999",
                OnboardingDtos.DeclaredRole.SINDICO,
                true,
                false,
                false,
                "web-landing",
                new OnboardingDtos.Attribution(
                        "/?utm_source=google",
                        "https://example.com",
                        "google",
                        "cpc",
                        "microcondominios",
                        "banner",
                        "condominios")));

        ArgumentCaptor<OnboardingLead> captor = ArgumentCaptor.forClass(OnboardingLead.class);
        verifySave(captor);
        OnboardingLead lead = captor.getValue();
        assertThat(ReflectionTestUtils.getField(lead, "utmSource")).isNull();
        assertThat(ReflectionTestUtils.getField(lead, "referrer")).isNull();
        assertThat(ReflectionTestUtils.getField(lead, "landingPath")).isNull();
        org.mockito.Mockito.verify(emails).queueNotifications(lead);
    }

    @Test
    void attributionIsStoredWhenAnalyticsConsentIsGranted() {
        service.capture(new OnboardingDtos.LeadRequest(
                "Maria da Silva",
                "Maria@example.com",
                "+55 11 99999-9999",
                OnboardingDtos.DeclaredRole.SINDICO,
                true,
                false,
                true,
                "web-landing",
                new OnboardingDtos.Attribution(
                        "/?utm_source=google",
                        "https://example.com",
                        "google",
                        "cpc",
                        "microcondominios",
                        "banner",
                        "condominios")));

        ArgumentCaptor<OnboardingLead> captor = ArgumentCaptor.forClass(OnboardingLead.class);
        verifySave(captor);
        OnboardingLead lead = captor.getValue();
        assertThat(ReflectionTestUtils.getField(lead, "utmSource")).isEqualTo("google");
        assertThat(ReflectionTestUtils.getField(lead, "referrer")).isEqualTo("https://example.com");
        assertThat(ReflectionTestUtils.getField(lead, "landingPath")).isEqualTo("/?utm_source=google");
        org.mockito.Mockito.verify(emails).queueNotifications(lead);
    }

    private void verifySave(ArgumentCaptor<OnboardingLead> captor) {
        org.mockito.Mockito.verify(leads).save(captor.capture());
    }
}
