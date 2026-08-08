package com.br.bz.kaza.kaza.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.br.bz.kaza.kaza.api.OnboardingDtos;
import com.br.bz.kaza.kaza.domain.OnboardingApplication;
import com.br.bz.kaza.kaza.domain.OnboardingApplicationStatus;
import com.br.bz.kaza.kaza.domain.User;
import com.br.bz.kaza.kaza.repository.OnboardingApplicationRepository;
import com.br.bz.kaza.kaza.repository.OnboardingDocumentRepository;
import com.br.bz.kaza.kaza.repository.OnboardingLeadRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

class OnboardingApplicationServiceTests {
    private OnboardingApplicationRepository applications;
    private OnboardingDocumentRepository documents;
    private UserService users;
    private OnboardingApplicationService service;
    private UUID applicationId;
    private OnboardingApplication application;

    @BeforeEach
    void setUp() {
        applications = mock(OnboardingApplicationRepository.class);
        documents = mock(OnboardingDocumentRepository.class);
        users = mock(UserService.class);
        service = new OnboardingApplicationService(
                applications,
                mock(OnboardingLeadRepository.class),
                documents,
                users,
                new BrazilianTaxIdValidator());
        applicationId = UUID.randomUUID();
        application = new OnboardingApplication(new User("owner-subject", "owner@kaza.test", "Owner"), null);
        when(applications.findById(applicationId)).thenReturn(Optional.of(application));
        when(users.ensureFromJwt(org.mockito.ArgumentMatchers.any())).thenReturn(application.getApplicant());
    }

    @Test
    void anotherApplicantCannotReadApplication() {
        assertThatThrownBy(() -> service.getEditable(applicationId, jwt("other-subject")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void invalidIdealFractionTotalCannotBeSubmitted() {
        populate(List.of(
                new OnboardingDtos.UnitDraftRequest("101", new BigDecimal("0.50000000")),
                new OnboardingDtos.UnitDraftRequest("102", new BigDecimal("0.40000000"))));
        when(documents.countByApplicationIdAndRetentionState(
                org.mockito.ArgumentMatchers.nullable(UUID.class),
                org.mockito.ArgumentMatchers.any())).thenReturn(1L);

        assertThatThrownBy(() -> service.submit(applicationId, jwt("owner-subject")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ideal fractions must total exactly 1.00000000");
        assertThat(application.getStatus()).isEqualTo(OnboardingApplicationStatus.DRAFT);
    }

    @Test
    void validDraftTransitionsToUnderReview() {
        populate(List.of(
                new OnboardingDtos.UnitDraftRequest("101", new BigDecimal("0.50000000")),
                new OnboardingDtos.UnitDraftRequest("102", new BigDecimal("0.50000000"))));
        when(documents.countByApplicationIdAndRetentionState(
                org.mockito.ArgumentMatchers.nullable(UUID.class),
                org.mockito.ArgumentMatchers.any())).thenReturn(1L);

        OnboardingApplication submitted = service.submit(applicationId, jwt("owner-subject"));

        assertThat(submitted.getStatus()).isEqualTo(OnboardingApplicationStatus.UNDER_REVIEW);
        assertThat(submitted.getSubmittedAt()).isNotNull();
    }

    private void populate(List<OnboardingDtos.UnitDraftRequest> units) {
        service.update(applicationId, new OnboardingDtos.UpdateApplicationRequest(
                "Maria da Silva",
                "maria@kaza.test",
                "+55 11 99999-9999",
                "52998224725",
                "Condomínio Kaza",
                "Rua das Flores, 10",
                "São Paulo",
                "SP",
                "01001000",
                units.size(),
                new BigDecimal("7.00"),
                units), jwt("owner-subject"));
    }

    private Jwt jwt(String subject) {
        return new Jwt(
                "token",
                java.time.Instant.now(),
                java.time.Instant.now().plusSeconds(300),
                Map.of("alg", "none"),
                Map.of("sub", subject, "email", subject + "@kaza.test"));
    }
}
