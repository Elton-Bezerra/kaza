package com.br.bz.kaza.kaza.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "expenses")
public class Expense {

    @Id
    @GeneratedValue
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "condominium_id", nullable = false)
    private Condominium condominium;
    @Column(nullable = false)
    private String description;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;
    @Column(name = "due_date", nullable = false)
    private java.time.LocalDate dueDate;
    @Column(name = "barcode")
    private String barcode;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING_APPROVAL;
    @Column(name = "approved_by")
    private String approvedBy;
    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;
    @Column(name = "asaas_payment_id")
    private String asaasPaymentId;

    protected Expense() {
    }

    public Expense(Condominium c, String description, BigDecimal amount, java.time.LocalDate dueDate, String barcode) {
        this.condominium = c;
        this.description = description;
        this.amount = amount;
        this.dueDate = dueDate;
        this.barcode = barcode;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCondominiumId() {
        return condominium.getId();
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public java.time.LocalDate getDueDate() {
        return dueDate;
    }

    public String getBarcode() {
        return barcode;
    }

    public Status getStatus() {
        return status;
    }

    public void approve(String subject, String paymentId) {
        status = Status.APPROVED;
        approvedBy = subject;
        approvedAt = OffsetDateTime.now();
        asaasPaymentId = paymentId;
    }

    public enum Status {PENDING_APPROVAL, APPROVED, PAID, REJECTED}
}
