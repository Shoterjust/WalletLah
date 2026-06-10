package com.walletlah.dashboard.api;

import com.walletlah.expense.Expense;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ExpenseResponse(
        Long id,
        BigDecimal amount,
        String category,
        String categoryDisplayName,
        String description,
        LocalDate expenseDate,
        String merchant,
        String source,
        Long recurringExpenseId,
        Instant createdAt
) {

    public static ExpenseResponse from(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getAmount(),
                expense.getCategory().name(),
                expense.getCategory().displayName(),
                expense.getDescription(),
                expense.getExpenseDate(),
                expense.getMerchant(),
                expense.getSource().name(),
                expense.getRecurringExpenseId(),
                expense.getCreatedAt()
        );
    }
}
