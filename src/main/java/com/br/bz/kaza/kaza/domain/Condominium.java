package com.br.bz.kaza.kaza.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "condominiums")
public class Condominium {

    @Id
    @GeneratedValue
    private UUID id;
    @Column(nullable = false)
    private String name;
    @Column(name = "tax_id", nullable = false, unique = true)
    private String taxId;
    @Column(name = "admin_subject", nullable = false)
    private String adminSubject;
    @Column(name = "approval_pin_hash", nullable = false)
    private String approvalPinHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING_ASAAS;
    @Column(name = "asaas_account_id")
    private String asaasAccountId;
    @Column(name = "asaas_wallet_id")
    private String asaasWalletId;
    @Column(name = "asaas_api_key")
    private String asaasApiKey;
    @Column(name = "subscription_price_per_unit", nullable = false, precision = 10, scale = 2)
    private BigDecimal subscriptionPricePerUnit;
    @OneToMany(mappedBy = "condominium", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Unit> units = new ArrayList<>();

    protected Condominium() {
    }

    public Condominium(String name, String taxId, String adminSubject, String approvalPinHash,
            BigDecimal subscriptionPricePerUnit) {
        this.name = name;
        this.taxId = taxId;
        this.adminSubject = adminSubject;
        this.approvalPinHash = approvalPinHash;
        this.subscriptionPricePerUnit = subscriptionPricePerUnit;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTaxId() {
        return taxId;
    }

    public String getAdminSubject() {
        return adminSubject;
    }

    public String getApprovalPinHash() {
        return approvalPinHash;
    }

    public Status getStatus() {
        return status;
    }

    public void activate(String accountId, String walletId, String apiKey) {
        this.asaasAccountId = accountId;
        this.asaasWalletId = walletId;
        this.asaasApiKey = apiKey;
        this.status = Status.ACTIVE;
    }

    public List<Unit> getUnits() {
        return units;
    }

    public String getAsaasAccountId() {
        return asaasAccountId;
    }

    public String getAsaasWalletId() {
        return asaasWalletId;
    }

    public String getAsaasApiKey() {
        return asaasApiKey;
    }

    public BigDecimal getSubscriptionPricePerUnit() {
        return subscriptionPricePerUnit;
    }

    public BigDecimal fractionTotal() {
        return units.stream().map(Unit::getIdealFraction).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public enum Status {PENDING_ASAAS, ACTIVE, SUSPENDED}
}
