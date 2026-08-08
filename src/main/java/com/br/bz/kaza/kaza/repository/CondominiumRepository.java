package com.br.bz.kaza.kaza.repository;

import com.br.bz.kaza.kaza.domain.Condominium;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CondominiumRepository extends JpaRepository<Condominium, UUID> {

    boolean existsByTaxId(String taxId);
}
