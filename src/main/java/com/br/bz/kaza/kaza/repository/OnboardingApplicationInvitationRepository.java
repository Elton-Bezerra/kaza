package com.br.bz.kaza.kaza.repository;

import com.br.bz.kaza.kaza.domain.OnboardingApplicationInvitation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OnboardingApplicationInvitationRepository extends JpaRepository<OnboardingApplicationInvitation, UUID> {
    Optional<OnboardingApplicationInvitation> findByTokenHash(String tokenHash);

    List<OnboardingApplicationInvitation> findByLeadIdOrderByCreatedAtDesc(UUID leadId);

    Optional<OnboardingApplicationInvitation> findFirstByApplicantSubjectOrderByCreatedAtDesc(String applicantSubject);
}
