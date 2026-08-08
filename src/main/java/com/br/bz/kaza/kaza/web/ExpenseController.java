package com.br.bz.kaza.kaza.web;

import com.br.bz.kaza.kaza.api.ApiDtos;
import com.br.bz.kaza.kaza.asaas.AsaasClient;
import com.br.bz.kaza.kaza.domain.Condominium;
import com.br.bz.kaza.kaza.domain.Expense;
import com.br.bz.kaza.kaza.repository.ExpenseRepository;
import com.br.bz.kaza.kaza.service.CondominiumService;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/condominiums/{condominiumId}/expenses")
public class ExpenseController {

    private final CondominiumService condos;
    private final ExpenseRepository expenses;
    private final AsaasClient asaas;

    public ExpenseController(CondominiumService condos, ExpenseRepository expenses, AsaasClient asaas) {
        this.condos = condos;
        this.expenses = expenses;
        this.asaas = asaas;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SINDICO', 'SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public UUID create(@PathVariable UUID condominiumId, @RequestBody ApiDtos.ExpenseRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Condominium c = condos.getForSyndic(condominiumId, jwt);
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        return expenses.save(new Expense(c, request.description(), request.amount(), request.dueDate(), request.barcode())).getId();
    }

    @PostMapping("/{expenseId}/approve")
    @PreAuthorize("hasAnyRole('SINDICO', 'SUPER_ADMIN')")
    public String approve(@PathVariable UUID condominiumId, @PathVariable UUID expenseId,
            @RequestBody ApiDtos.ApprovalRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Condominium c = condos.getForSyndic(condominiumId, jwt);
        Expense expense = expenses.findById(expenseId).orElseThrow(() -> new IllegalArgumentException("Expense not found"));
        if (!condominiumId.equals(expense.getCondominiumId())) {
            throw new IllegalArgumentException("Expense not found");
        }
        if (!condos.matchesPin(c, request.pin())) {
            throw new AccessDeniedException("Invalid approval PIN");
        }
        if (expense.getStatus() != Expense.Status.PENDING_APPROVAL) {
            throw new IllegalStateException("Expense is not pending approval");
        }
        expense.approve(jwt.getSubject(), null);
        expenses.save(expense);
        return expense.getStatus().name();
    }
}
