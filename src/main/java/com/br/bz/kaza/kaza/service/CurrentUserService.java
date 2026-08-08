package com.br.bz.kaza.kaza.service;

import com.br.bz.kaza.kaza.api.OnboardingDtos;
import com.br.bz.kaza.kaza.domain.CondominiumMembership;
import com.br.bz.kaza.kaza.domain.OnboardingApplication;
import com.br.bz.kaza.kaza.domain.User;
import com.br.bz.kaza.kaza.repository.CondominiumMembershipRepository;
import com.br.bz.kaza.kaza.repository.OnboardingApplicationRepository;
import java.util.List;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrentUserService {
    private final UserService users;
    private final CondominiumMembershipRepository memberships;
    private final OnboardingApplicationRepository applications;

    public CurrentUserService(UserService users, CondominiumMembershipRepository memberships,
            OnboardingApplicationRepository applications) {
        this.users = users;
        this.memberships = memberships;
        this.applications = applications;
    }

    @Transactional
    public OnboardingDtos.MeResponse get(Jwt jwt) {
        User user = users.ensureFromJwt(jwt);
        List<OnboardingDtos.MembershipResponse> membershipResponses = memberships
                .findByUserSubjectAndActiveTrueOrderByCreatedAt(user.getSubject())
                .stream()
                .map(this::membershipResponse)
                .toList();
        OnboardingDtos.OnboardingSummary onboarding = applications
                .findFirstByApplicantSubjectOrderByUpdatedAtDesc(user.getSubject())
                .map(this::onboardingSummary)
                .orElse(null);
        return new OnboardingDtos.MeResponse(
                user.getId(),
                user.getSubject(),
                user.getEmail(),
                user.getDisplayName(),
                membershipResponses,
                onboarding);
    }

    private OnboardingDtos.MembershipResponse membershipResponse(CondominiumMembership membership) {
        return new OnboardingDtos.MembershipResponse(
                membership.getCondominium().getId(),
                membership.getCondominium().getName(),
                membership.getRole().name());
    }

    private OnboardingDtos.OnboardingSummary onboardingSummary(OnboardingApplication application) {
        return new OnboardingDtos.OnboardingSummary(
                application.getId(), application.getStatus().name(), application.getReviewReason());
    }
}
