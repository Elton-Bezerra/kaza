package com.br.bz.kaza.kaza.repository;

import com.br.bz.kaza.kaza.domain.OnboardingDocumentObject;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OnboardingDocumentObjectRepository extends JpaRepository<OnboardingDocumentObject, UUID> {
}
