package com.walletlah.expense;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecentExpenseView(
        BigDecimal amount,
        ExpenseCategory category,
        String description,
        LocalDate expenseDate,
        ExpenseSource source
) {
    public static RecentExpenseView from(Expense expense) {
        return new RecentExpenseView(
                expense.getAmount(),
                expense.getCategory(),
                expense.getDescription(),
                expense.getExpenseDate(),
                expense.getSource()
        );
    }
}
