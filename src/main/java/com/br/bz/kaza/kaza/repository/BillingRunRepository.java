package com.br.bz.kaza.kaza.repository;

import com.br.bz.kaza.kaza.domain.BillingRun;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingRunRepository extends JpaRepository<BillingRun, UUID> {
    Optional<BillingRun> findByCondominiumIdAndPeriod(UUID condominiumId, String period);
}
