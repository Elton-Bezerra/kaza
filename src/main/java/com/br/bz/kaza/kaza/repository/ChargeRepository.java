package com.br.bz.kaza.kaza.repository;

import com.br.bz.kaza.kaza.domain.Charge;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChargeRepository extends JpaRepository<Charge, UUID> {
    long countByBillingRunId(UUID billingRunId);
}
