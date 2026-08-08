package com.br.bz.kaza.kaza.repository;

import com.br.bz.kaza.kaza.domain.LeadEmailDelivery;
import com.br.bz.kaza.kaza.domain.LeadEmailDeliveryStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeadEmailDeliveryRepository extends JpaRepository<LeadEmailDelivery, UUID> {
    List<LeadEmailDelivery> findByLeadIdAndStatusOrderByCreatedAtAsc(UUID leadId, LeadEmailDeliveryStatus status);

    List<LeadEmailDelivery> findByLeadIdOrderByCreatedAtAsc(UUID leadId);
}
