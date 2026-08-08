package com.br.bz.kaza.kaza.service;

import com.br.bz.kaza.kaza.api.ApiDtos;
import com.br.bz.kaza.kaza.asaas.AsaasClient;
import com.br.bz.kaza.kaza.domain.Condominium;
import com.br.bz.kaza.kaza.domain.Unit;
import com.br.bz.kaza.kaza.domain.MembershipRole;
import com.br.bz.kaza.kaza.domain.User;
import com.br.bz.kaza.kaza.repository.CondominiumRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CondominiumService {

    private final CondominiumRepository repository;
    private final AsaasClient asaas;
    private final UserService users;
    private final CondominiumMembershipService memberships;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public CondominiumService(CondominiumRepository repository, AsaasClient asaas,
            UserService users, CondominiumMembershipService memberships) {
        this.repository = repository;
        this.asaas = asaas;
        this.users = users;
        this.memberships = memberships;
    }

    @Transactional
    public Condominium create(ApiDtos.CondominiumRequest request, Jwt jwt) {
        User actor = users.ensureFromJwt(jwt);
        if (request.units() == null || request.units().isEmpty()) {
            throw new IllegalArgumentException("At least one unit is required");
        }
        if (request.approvalPin() == null || !request.approvalPin().matches("\\d{6}")) {
            throw new IllegalArgumentException("Approval PIN must have 6 digits");
        }
        if (request.subscriptionPricePerUnit() == null
                || request.subscriptionPricePerUnit().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Subscription price per unit must be positive");
        }
        if (repository.existsByTaxId(request.taxId())) {
            throw new IllegalArgumentException("Tax ID already registered");
        }
        if (request.units().stream().anyMatch(unit -> unit.idealFraction() == null || unit.identifier() == null
                || unit.residentTaxId() == null || unit.residentName() == null)) {
            throw new IllegalArgumentException("Each unit needs an identifier, ideal fraction, resident name, and resident tax ID");
        }

        BigDecimal total = request.units().stream().map(ApiDtos.UnitRequest::idealFraction)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(BigDecimal.ONE.setScale(8)) != 0) {
            throw new IllegalArgumentException("Ideal fractions must total exactly 1.00000000");
        }

        Condominium condo = new Condominium(request.name(), request.taxId(), actor.getSubject(),
                encoder.encode(request.approvalPin()), request.subscriptionPricePerUnit().setScale(2, RoundingMode.UNNECESSARY));
        request.units().forEach(unit -> condo.getUnits().add(
                        new Unit(
                                condo,
                                unit.identifier(),
                                unit.idealFraction().setScale(8, RoundingMode.UNNECESSARY),
                                unit.residentSubject(),
                                unit.residentName(),
                                unit.residentTaxId(), validBillingType(unit.billingType()))
                )
        );
        AsaasClient.Account account = asaas.createSubaccount(request.name(), request.adminEmail(), request.taxId());
        condo.activate(account.id(), account.walletId(), account.apiKey());
        Condominium saved = repository.save(condo);
        memberships.ensure(actor, saved, MembershipRole.SINDICO);
        request.units().forEach(unit -> {
            User resident = users.ensureFromSubject(unit.residentSubject(), unit.residentName());
            memberships.ensure(resident, saved, MembershipRole.MORADOR);
        });
        return saved;
    }

    public Condominium getForMember(UUID id, Jwt jwt) {
        users.ensureFromJwt(jwt);
        Condominium condo = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Condominium not found"));
        if (!isSuperAdmin(jwt)) {
            memberships.requireMember(jwt.getSubject(), id);
        }
        return condo;
    }

    public Condominium getForSyndic(UUID id, Jwt jwt) {
        users.ensureFromJwt(jwt);
        Condominium condo = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Condominium not found"));
        if (!isSuperAdmin(jwt)) {
            memberships.requireRole(jwt.getSubject(), id, MembershipRole.SINDICO);
        }
        return condo;
    }

    private boolean isSuperAdmin(Jwt jwt) {
        Object realmAccess = jwt.getClaims().get("realm_access");
        if (realmAccess instanceof java.util.Map<?, ?> claims
                && claims.get("roles") instanceof java.util.Collection<?> roles) {
            return roles.stream().anyMatch(role -> "SUPER_ADMIN".equals(String.valueOf(role)));
        }
        return false;
    }

    public boolean matchesPin(Condominium condo, String pin) {
        return encoder.matches(pin, condo.getApprovalPinHash());
    }

    public String validBillingType(String billingType) {
        String value = billingType == null ? "PIX" : billingType;
        if (!java.util.Set.of("PIX", "BOLETO", "CREDIT_CARD").contains(value)) {
            throw new IllegalArgumentException("Billing type must be PIX, BOLETO, or CREDIT_CARD");
        }
        return value;
    }
}
