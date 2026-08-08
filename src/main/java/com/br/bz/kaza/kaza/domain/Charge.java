package com.br.bz.kaza.kaza.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "charges", uniqueConstraints = {
        @UniqueConstraint(name = "uk_charge_external", columnNames = "asaas_payment_id"),
        @UniqueConstraint(name = "uk_charge_run_unit", columnNames = {"billing_run_id", "unit_id"})
})
public class Charge {

    @Id
    @GeneratedValue
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "condominium_id", nullable = false)
    private Condominium condominium;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "billing_run_id", nullable = false)
    private BillingRun billingRun;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;
    @Column(name = "billing_type", nullable = false)
    private String billingType;
    @Column(name = "asaas_payment_id")
    private String asaasPaymentId;
    @Column(nullable = false)
    private String status;

    protected Charge() {
    }

    public Charge(Condominium c, Unit u, BillingRun billingRun, BigDecimal amount, LocalDate dueDate, String billingType, String paymentId,
            String status) {
        this.condominium = c;
        this.unit = u;
        this.billingRun = billingRun;
        this.amount = amount;
        this.dueDate = dueDate;
        this.billingType = billingType;
        this.asaasPaymentId = paymentId;
        this.status = status;
    }
}
