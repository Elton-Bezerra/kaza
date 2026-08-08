package com.br.bz.kaza.kaza.repository;

import com.br.bz.kaza.kaza.domain.CondominiumMembership;
import com.br.bz.kaza.kaza.domain.MembershipRole;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CondominiumMembershipRepository extends JpaRepository<CondominiumMembership, UUID> {
    boolean existsByUserSubjectAndCondominiumIdAndActiveTrue(String subject, UUID condominiumId);
    boolean existsByUserSubjectAndCondominiumIdAndRoleAndActiveTrue(
            String subject, UUID condominiumId, MembershipRole role);
    List<CondominiumMembership> findByUserSubjectAndActiveTrueOrderByCreatedAt(String subject);
}
