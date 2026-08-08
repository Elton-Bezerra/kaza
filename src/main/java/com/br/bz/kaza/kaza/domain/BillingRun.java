package com.br.bz.kaza.kaza.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "billing_runs",
        uniqueConstraints = @UniqueConstraint(name = "uk_billing_run_period", columnNames = {"condominium_id", "period"}))
public class BillingRun {
    @Id @GeneratedValue private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "condominium_id", nullable = false) private Condominium condominium;
    @Column(nullable = false, length = 7) private String period;
    @Column(name = "expenses_total", nullable = false, precision = 14, scale = 2) private BigDecimal expensesTotal;
    @Column(name = "subscription_total", nullable = false, precision = 14, scale = 2) private BigDecimal subscriptionTotal;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal total;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Status status = Status.CREATED;
    @OneToMany(mappedBy = "billingRun", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Charge> charges = new ArrayList<>();

    public enum Status { CREATED, CHARGES_CREATED, PARTIALLY_FAILED, RECONCILED }
    protected BillingRun() {}
    public BillingRun(Condominium condominium, YearMonth period, BigDecimal expensesTotal,
                      BigDecimal subscriptionTotal, BigDecimal total) {
        this.condominium = condominium; this.period = period.toString(); this.expensesTotal = expensesTotal;
        this.subscriptionTotal = subscriptionTotal; this.total = total;
    }
    public UUID getId() { return id; }
    public String getPeriod() { return period; }
    public BigDecimal getExpensesTotal() { return expensesTotal; }
    public BigDecimal getSubscriptionTotal() { return subscriptionTotal; }
    public BigDecimal getTotal() { return total; }
    public Status getStatus() { return status; }
    public List<Charge> getCharges() { return charges; }
    public void markChargesCreated() { status = Status.CHARGES_CREATED; }
}
