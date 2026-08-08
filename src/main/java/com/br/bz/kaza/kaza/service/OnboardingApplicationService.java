package com.br.bz.kaza.kaza.service;

import com.br.bz.kaza.kaza.api.OnboardingDtos;
import com.br.bz.kaza.kaza.domain.OnboardingApplication;
import com.br.bz.kaza.kaza.domain.OnboardingApplicationStatus;
import com.br.bz.kaza.kaza.domain.OnboardingApplicationUnit;
import com.br.bz.kaza.kaza.domain.OnboardingDocument.RetentionState;
import com.br.bz.kaza.kaza.domain.OnboardingLead;
import com.br.bz.kaza.kaza.domain.User;
import com.br.bz.kaza.kaza.repository.OnboardingApplicationRepository;
import com.br.bz.kaza.kaza.repository.OnboardingDocumentRepository;
import com.br.bz.kaza.kaza.repository.OnboardingLeadRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OnboardingApplicationService {
    private static final Set<OnboardingApplicationStatus> OPEN_STATUSES = Set.of(
            OnboardingApplicationStatus.DRAFT,
            OnboardingApplicationStatus.UNDER_REVIEW,
            OnboardingApplicationStatus.NEEDS_MORE_INFORMATION,
            OnboardingApplicationStatus.APPROVED,
            OnboardingApplicationStatus.ACTIVATING,
            OnboardingApplicationStatus.KAZACONTA_PENDING);

    private final OnboardingApplicationRepository applications;
    private final OnboardingLeadRepository leads;
    private final OnboardingDocumentRepository documents;
    private final UserService users;
    private final BrazilianTaxIdValidator taxIds;

    public OnboardingApplicationService(OnboardingApplicationRepository applications,
            OnboardingLeadRepository leads, OnboardingDocumentRepository documents,
            UserService users, BrazilianTaxIdValidator taxIds) {
        this.applications = applications;
        this.leads = leads;
        this.documents = documents;
        this.users = users;
        this.taxIds = taxIds;
    }

    @Transactional
    public OnboardingApplication create(OnboardingDtos.CreateApplicationRequest request, Jwt jwt) {
        User applicant = users.ensureFromJwt(jwt);
        if (applications.existsByApplicantIdAndStatusIn(applicant.getId(), OPEN_STATUSES)) {
            throw new IllegalStateException("Applicant already has an open onboarding application");
        }
        OnboardingLead lead = request != null && request.leadId() != null
                ? requireOwnedLead(request.leadId(), jwt)
                : null;
        return applications.save(new OnboardingApplication(applicant, lead));
    }

    @Transactional
    public OnboardingApplication getEditable(UUID id, Jwt jwt) {
        users.ensureFromJwt(jwt);
        OnboardingApplication application = requireOwned(id, jwt.getSubject());
        requireEditable(application);
        application.getUnits().size();
        return application;
    }

    @Transactional
    public OnboardingApplication update(UUID id, OnboardingDtos.UpdateApplicationRequest request, Jwt jwt) {
        users.ensureFromJwt(jwt);
        OnboardingApplication application = requireOwned(id, jwt.getSubject());
        requireEditable(application);

        List<OnboardingApplicationUnit> replacementUnits = null;
        if (request.units() != null) {
            replacementUnits = new ArrayList<>();
            for (int index = 0; index < request.units().size(); index++) {
                OnboardingDtos.UnitDraftRequest unit = request.units().get(index);
                BigDecimal fraction = scaleFraction(unit.idealFraction());
                replacementUnits.add(new OnboardingApplicationUnit(
                        application, unit.identifier().trim(), fraction, index));
            }
        }

        application.update(
                valueOrExisting(request.responsibleName(), application.getResponsibleName()),
                valueOrExisting(request.responsibleEmail(), application.getResponsibleEmail()),
                valueOrExisting(request.responsiblePhone(), application.getResponsiblePhone()),
                taxIdOrExisting(request.taxId(), application.getTaxId()),
                valueOrExisting(request.condominiumName(), application.getCondominiumName()),
                valueOrExisting(request.addressLine(), application.getAddressLine()),
                valueOrExisting(request.addressCity(), application.getAddressCity()),
                stateOrExisting(request.addressState(), application.getAddressState()),
                valueOrExisting(request.postalCode(), application.getPostalCode()),
                request.proposedUnitCount() == null
                        ? application.getProposedUnitCount() : request.proposedUnitCount(),
                priceOrExisting(request.subscriptionPricePerUnit(), application.getSubscriptionPricePerUnit()),
                replacementUnits);
        application.getUnits().size();
        return application;
    }

    @Transactional
    public OnboardingApplication submit(UUID id, Jwt jwt) {
        users.ensureFromJwt(jwt);
        OnboardingApplication application = requireOwned(id, jwt.getSubject());
        requireEditable(application);
        validateForSubmission(application);
        application.submit();
        application.getUnits().size();
        return application;
    }

    @Transactional
    public OnboardingApplication requireEditableOwned(UUID id, Jwt jwt) {
        users.ensureFromJwt(jwt);
        OnboardingApplication application = requireOwned(id, jwt.getSubject());
        requireEditable(application);
        return application;
    }

    @Transactional(readOnly = true)
    public OnboardingApplication getOwned(UUID id, Jwt jwt) {
        users.ensureFromJwt(jwt);
        return requireOwned(id, jwt.getSubject());
    }

    @Transactional(readOnly = true)
    public Optional<OnboardingApplication> findLatestByApplicantSubject(String subject) {
        return applications.findFirstByApplicantSubjectOrderByUpdatedAtDesc(subject);
    }

    @Transactional(readOnly = true)
    public OnboardingApplication getById(UUID id) {
        return applications.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Onboarding application not found"));
    }

    private OnboardingLead requireOwnedLead(UUID leadId, Jwt jwt) {
        OnboardingLead lead = leads.findById(leadId)
                .orElseThrow(() -> new IllegalArgumentException("Onboarding lead not found"));
        String jwtEmail = jwt.getClaimAsString("email");
        if (jwtEmail == null || !lead.getEmail().equalsIgnoreCase(jwtEmail.trim())) {
            throw new AccessDeniedException("Onboarding lead does not belong to the authenticated applicant");
        }
        return lead;
    }

    private OnboardingApplication requireOwned(UUID id, String subject) {
        OnboardingApplication application = applications.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Onboarding application not found"));
        if (!application.getApplicant().getSubject().equals(subject)) {
            throw new AccessDeniedException("Onboarding application belongs to another applicant");
        }
        return application;
    }

    private void requireEditable(OnboardingApplication application) {
        if (!application.isEditable()) {
            throw new AccessDeniedException("Application is not available for applicant editing");
        }
    }

    private void validateForSubmission(OnboardingApplication application) {
        requireText(application.getResponsibleName(), "Responsible person name is required");
        requireText(application.getResponsibleEmail(), "Responsible person email is required");
        requireText(application.getResponsiblePhone(), "Responsible person phone is required");
        requireText(application.getCondominiumName(), "Condominium name is required");
        requireText(application.getAddressLine(), "Condominium address is required");
        requireText(application.getAddressCity(), "Condominium city is required");
        requireText(application.getAddressState(), "Condominium state is required");
        requireText(application.getPostalCode(), "Condominium postal code is required");
        if (!taxIds.isValid(application.getTaxId())) {
            throw new IllegalArgumentException("CPF or CNPJ is invalid");
        }
        if (application.getProposedUnitCount() == null || application.getProposedUnitCount() <= 0
                || application.getProposedUnitCount() > 500) {
            throw new IllegalArgumentException("Unit count must be between 1 and 500");
        }
        if (application.getSubscriptionPricePerUnit() == null
                || application.getSubscriptionPricePerUnit().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Subscription price per unit must be positive");
        }
        if (application.getUnits().size() != application.getProposedUnitCount()) {
            throw new IllegalArgumentException("Unit draft count must match the proposed unit count");
        }
        Set<String> identifiers = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO.setScale(8);
        for (OnboardingApplicationUnit unit : application.getUnits()) {
            String normalized = unit.getIdentifier().trim().toLowerCase(Locale.ROOT);
            if (!identifiers.add(normalized)) {
                throw new IllegalArgumentException("Unit identifiers must be unique");
            }
            BigDecimal fraction;
            try {
                fraction = unit.getIdealFraction().setScale(8, RoundingMode.UNNECESSARY);
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException("Ideal fractions may have at most 8 decimal places");
            }
            if (fraction.compareTo(BigDecimal.ZERO) <= 0 || fraction.compareTo(BigDecimal.ONE) > 0) {
                throw new IllegalArgumentException("Each ideal fraction must be greater than zero and at most one");
            }
            total = total.add(fraction);
        }
        if (total.compareTo(new BigDecimal("1.00000000")) != 0) {
            throw new IllegalArgumentException("Ideal fractions must total exactly 1.00000000");
        }
        if (documents.countByApplicationIdAndRetentionState(
                application.getId(), RetentionState.ACTIVE) == 0) {
            throw new IllegalArgumentException("At least one supporting document is required");
        }
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private String valueOrExisting(String value, String existing) {
        return value == null ? existing : value.trim();
    }

    private String taxIdOrExisting(String value, String existing) {
        return value == null ? existing : value.replaceAll("\\D", "");
    }

    private String stateOrExisting(String value, String existing) {
        return value == null ? existing : value.trim().toUpperCase(Locale.ROOT);
    }

    private BigDecimal priceOrExisting(BigDecimal value, BigDecimal existing) {
        if (value == null) {
            return existing;
        }
        try {
            return value.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Subscription price may have at most 2 decimal places");
        }
    }

    private BigDecimal scaleFraction(BigDecimal value) {
        try {
            return value.setScale(8, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Ideal fractions may have at most 8 decimal places");
        }
    }
}
