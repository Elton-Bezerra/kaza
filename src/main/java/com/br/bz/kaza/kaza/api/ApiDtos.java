package com.br.bz.kaza.kaza.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ApiDtos {

    private ApiDtos() {
    }

    public record UnitRequest(String identifier, BigDecimal idealFraction, String residentSubject,
                              String residentName, String residentTaxId, String billingType) {

    }

    public record CondominiumRequest(String name, String taxId, String adminEmail, String approvalPin,
                                     BigDecimal subscriptionPricePerUnit, List<UnitRequest> units) {

    }

    public record CondominiumResponse(UUID id, String name, String taxId, String status, BigDecimal fractionTotal) {

    }

    public record BillingRunRequest(String period, LocalDate dueDate) {

    }

    public record BillingRunResponse(UUID id, String period, BigDecimal expensesTotal,
                                     BigDecimal subscriptionTotal, BigDecimal total, String status,
                                     int chargesCreated) {
    }

    public record ExpenseRequest(String description, BigDecimal amount, LocalDate dueDate, String barcode) {

    }

    public record ApprovalRequest(String pin) {

    }

    public record PaymentMethodRequest(String billingType) {
    }
}
