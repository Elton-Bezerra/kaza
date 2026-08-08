package com.br.bz.kaza.kaza.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "condominium_memberships",
        uniqueConstraints = @UniqueConstraint(name = "uk_membership_user_condominium", columnNames = {"user_id", "condominium_id"}))
public class CondominiumMembership {
    @Id @GeneratedValue private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "condominium_id", nullable = false) private Condominium condominium;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private MembershipRole role;
    @Column(nullable = false) private boolean active = true;
    @Column(nullable = false) private OffsetDateTime createdAt;
    @Column(nullable = false) private OffsetDateTime updatedAt;

    protected CondominiumMembership() {}
    public CondominiumMembership(User user, Condominium condominium, MembershipRole role) {
        this.user = user; this.condominium = condominium; this.role = role;
    }
    @PrePersist void onCreate() { createdAt = OffsetDateTime.now(); updatedAt = createdAt; }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(); }
    public MembershipRole getRole() { return role; }
    public boolean isActive() { return active; }
}
