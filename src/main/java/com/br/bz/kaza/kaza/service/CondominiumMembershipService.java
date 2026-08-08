package com.br.bz.kaza.kaza.service;

import com.br.bz.kaza.kaza.domain.*;
import com.br.bz.kaza.kaza.repository.CondominiumMembershipRepository;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CondominiumMembershipService {
    private final CondominiumMembershipRepository memberships;
    public CondominiumMembershipService(CondominiumMembershipRepository memberships) { this.memberships = memberships; }

    @Transactional
    public void ensure(User user, Condominium condominium, MembershipRole role) {
        if (user != null && !memberships.existsByUserSubjectAndCondominiumIdAndActiveTrue(
                user.getSubject(), condominium.getId())) {
            memberships.save(new CondominiumMembership(user, condominium, role));
        }
    }

    public void requireMember(String subject, UUID condominiumId) {
        if (!memberships.existsByUserSubjectAndCondominiumIdAndActiveTrue(subject, condominiumId)) {
            throw new AccessDeniedException("User is not a member of this condominium");
        }
    }

    public void requireRole(String subject, UUID condominiumId, MembershipRole role) {
        if (!memberships.existsByUserSubjectAndCondominiumIdAndRoleAndActiveTrue(subject, condominiumId, role)) {
            throw new AccessDeniedException("User does not have the required condominium role");
        }
    }
}
