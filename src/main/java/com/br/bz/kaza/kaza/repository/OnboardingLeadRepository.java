package com.br.bz.kaza.kaza.repository;

import com.br.bz.kaza.kaza.domain.OnboardingLead;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OnboardingLeadRepository extends JpaRepository<OnboardingLead, UUID> {
    Optional<OnboardingLead> findFirstByEmailIgnoreCaseAndCreatedAtAfterOrderByCreatedAtDesc(
            String email, OffsetDateTime createdAfter);
    Page<OnboardingLead> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<OnboardingLead> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByCreatedAtDesc(
            String name, String email, Pageable pageable);
}
