package com.walletlah.expense;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AddExpenseRequest(
        BigDecimal amount,
        ExpenseCategory category,
        String description,
        LocalDate expenseDate
) {
}
