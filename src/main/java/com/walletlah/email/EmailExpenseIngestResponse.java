package com.walletlah.email;

import com.walletlah.expense.Expense;
import java.math.BigDecimal;
import java.time.LocalDate;

public record EmailExpenseIngestResponse(
        String status,
        String message,
        BigDecimal amount,
        String category,
        String description,
        LocalDate expenseDate
) {

    static EmailExpenseIngestResponse logged(Expense expense) {
        return new EmailExpenseIngestResponse(
                "logged",
                "Expense logged from email.",
                expense.getAmount(),
                expense.getCategory().displayName(),
                expense.getDescription(),
                expense.getExpenseDate()
        );
    }

    static EmailExpenseIngestResponse duplicate(String message) {
        return new EmailExpenseIngestResponse("duplicate", message, null, null, null, null);
    }

    static EmailExpenseIngestResponse rejected(String message) {
        return new EmailExpenseIngestResponse("rejected", message, null, null, null, null);
    }
}
