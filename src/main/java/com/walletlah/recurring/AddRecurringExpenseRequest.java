package com.walletlah.recurring;

import com.walletlah.expense.ExpenseCategory;
import java.math.BigDecimal;
import java.time.LocalDate;

public record AddRecurringExpenseRequest(
        BigDecimal amount,
        ExpenseCategory category,
        String description,
        String merchant,
        RecurringFrequency frequency,
        LocalDate nextRunDate
) {
}
