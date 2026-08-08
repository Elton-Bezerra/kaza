package com.br.bz.kaza.kaza.repository;

import com.br.bz.kaza.kaza.domain.Expense;
import java.util.UUID;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
    List<Expense> findByCondominiumIdAndDueDateBetweenAndStatusIn(
            UUID condominiumId, LocalDate start, LocalDate end, List<Expense.Status> statuses);
}
