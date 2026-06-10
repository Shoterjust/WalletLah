package com.walletlah.expense;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecentExpenseView(
        Long id,
        BigDecimal amount,
        ExpenseCategory category,
        String description,
        LocalDate expenseDate,
        ExpenseSource source
) {
    public static RecentExpenseView from(Expense expense) {
        return new RecentExpenseView(
                expense.getId(),
                expense.getAmount(),
                expense.getCategory(),
                expense.getDescription(),
                expense.getExpenseDate(),
                expense.getSource()
        );
    }
}
