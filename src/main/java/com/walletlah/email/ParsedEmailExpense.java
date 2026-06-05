package com.walletlah.email;

import com.walletlah.expense.ExpenseCategory;
import java.math.BigDecimal;
import java.time.LocalDate;

record ParsedEmailExpense(
        BigDecimal amount,
        ExpenseCategory category,
        String description,
        LocalDate expenseDate
) {
}
