package com.br.bz.kaza.kaza.repository;

import com.br.bz.kaza.kaza.domain.OnboardingDocument;
import com.br.bz.kaza.kaza.domain.OnboardingDocument.RetentionState;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OnboardingDocumentRepository extends JpaRepository<OnboardingDocument, UUID> {
    List<OnboardingDocument> findByApplicationIdAndRetentionStateOrderByUploadedAt(
            UUID applicationId, RetentionState retentionState);
    Optional<OnboardingDocument> findByIdAndApplicationId(UUID id, UUID applicationId);
    long countByApplicationIdAndRetentionState(UUID applicationId, RetentionState retentionState);
}
