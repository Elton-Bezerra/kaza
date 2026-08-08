package com.br.bz.kaza.kaza.repository;

import com.br.bz.kaza.kaza.domain.OnboardingApplication;
import com.br.bz.kaza.kaza.domain.OnboardingApplicationStatus;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OnboardingApplicationRepository extends JpaRepository<OnboardingApplication, UUID> {
    boolean existsByApplicantIdAndStatusIn(UUID applicantId, Collection<OnboardingApplicationStatus> statuses);
    Optional<OnboardingApplication> findFirstByApplicantSubjectOrderByUpdatedAtDesc(String subject);
    Page<OnboardingApplication> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<OnboardingApplication> findByStatusOrderByCreatedAtDesc(OnboardingApplicationStatus status, Pageable pageable);
}
