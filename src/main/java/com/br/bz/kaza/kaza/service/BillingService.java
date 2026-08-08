package com.br.bz.kaza.kaza.service;

import com.br.bz.kaza.kaza.api.ApiDtos;
import com.br.bz.kaza.kaza.asaas.AsaasClient;
import com.br.bz.kaza.kaza.domain.BillingRun;
import com.br.bz.kaza.kaza.domain.Charge;
import com.br.bz.kaza.kaza.domain.Condominium;
import com.br.bz.kaza.kaza.domain.Expense;
import com.br.bz.kaza.kaza.domain.Unit;
import com.br.bz.kaza.kaza.repository.BillingRunRepository;
import com.br.bz.kaza.kaza.repository.ExpenseRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingService {
    private final BillingRunRepository runs;
    private final ExpenseRepository expenses;
    private final AsaasClient asaas;
    public BillingService(BillingRunRepository runs, ExpenseRepository expenses, AsaasClient asaas) {
        this.runs = runs;
        this.expenses = expenses;
        this.asaas = asaas;
    }

    @Transactional
    public BillingRun create(Condominium condominium, ApiDtos.BillingRunRequest request) {
        if (request.period() == null || request.dueDate() == null) {
            throw new IllegalArgumentException("Billing period and due date are required");
        }
        YearMonth period;
        try {
            period = YearMonth.parse(request.period());
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Period must use YYYY-MM format");
        }
        if (runs.findByCondominiumIdAndPeriod(condominium.getId(), period.toString()).isPresent()) {
            throw new IllegalStateException("Billing run already exists for " + period);
        }
        LocalDate start = period.atDay(1);
        LocalDate end = period.atEndOfMonth();
        BigDecimal expenseTotal = expenses.findByCondominiumIdAndDueDateBetweenAndStatusIn(
                condominium.getId(), start, end, List.of(Expense.Status.APPROVED, Expense.Status.PAID))
                .stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        BigDecimal subscriptionPricePerUnit = condominium.getSubscriptionPricePerUnit();
        BigDecimal subscriptionTotal = subscriptionPricePerUnit.multiply(BigDecimal.valueOf(condominium.getUnits().size()));
        BigDecimal total = expenseTotal.add(subscriptionTotal).setScale(2, RoundingMode.HALF_UP);
        BillingRun run = runs.save(new BillingRun(condominium, period, expenseTotal, subscriptionTotal, total));
        BigDecimal allocated = BigDecimal.ZERO;
        for (int index = 0; index < condominium.getUnits().size(); index++) {
            Unit unit = condominium.getUnits().get(index);
            BigDecimal amount = expenseTotal.multiply(unit.getIdealFraction())
                    .add(subscriptionPricePerUnit).setScale(2, RoundingMode.HALF_UP);
            if (index == condominium.getUnits().size() - 1) {
                amount = total.subtract(allocated).setScale(2, RoundingMode.HALF_UP);
            }
            allocated = allocated.add(amount);
            String paymentId = null;
            String status = "PENDING_PROVIDER";
            if (asaas.isConfigured(condominium.getAsaasApiKey())) {
                if (unit.getAsaasCustomerId() == null) {
                    unit.setAsaasCustomerId(asaas.createCustomer(condominium.getAsaasApiKey(),
                            unit.getResidentName(), unit.getResidentTaxId()).id());
                }
                var payment = asaas.createPayment(condominium.getAsaasApiKey(), unit.getAsaasCustomerId(),
                        amount, request.dueDate(), unit.getBillingType());
                paymentId = payment.id();
                status = payment.status();
            }
            run.getCharges().add(new Charge(condominium, unit, run, amount, request.dueDate(),
                    unit.getBillingType(), paymentId, status));
        }
        run.markChargesCreated();
        return runs.save(run);
    }
}
