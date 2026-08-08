package com.br.bz.kaza.kaza.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "units")

public class Unit {

    @Id
    @GeneratedValue
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "condominium_id", nullable = false)
    private Condominium condominium;
    @Column(nullable = false)
    private String identifier;
    @Column(name = "ideal_fraction", nullable = false, precision = 12, scale = 8)
    private BigDecimal idealFraction;
    @Column(name = "resident_subject")
    private String residentSubject;
    @Column(name = "resident_name")
    private String residentName;
    @Column(name = "resident_tax_id")
    private String residentTaxId;
    @Column(name = "asaas_customer_id")
    private String asaasCustomerId;
    @Column(name = "billing_type", nullable = false)
    private String billingType;

    protected Unit() {
    }

    public Unit(Condominium condominium, String identifier, BigDecimal idealFraction, String residentSubject,
            String residentName, String residentTaxId, String billingType) {
        this.condominium = condominium;
        this.identifier = identifier;
        this.idealFraction = idealFraction;
        this.residentSubject = residentSubject;
        this.residentName = residentName;
        this.residentTaxId = residentTaxId;
        this.billingType = billingType;
    }

    public UUID getId() {
        return id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public BigDecimal getIdealFraction() {
        return idealFraction;
    }

    public String getResidentSubject() {
        return residentSubject;
    }

    public String getResidentName() {
        return residentName;
    }

    public String getResidentTaxId() {
        return residentTaxId;
    }

    public String getAsaasCustomerId() {
        return asaasCustomerId;
    }

    public void setAsaasCustomerId(String id) {
        this.asaasCustomerId = id;
    }

    public String getBillingType() {
        return billingType;
    }

    public void setBillingType(String billingType) {
        this.billingType = billingType;
    }
}
