package com.walletlah.email;

import com.walletlah.receipt.PendingExpense;
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

    static EmailExpenseIngestResponse pending(PendingExpense pendingExpense) {
        return new EmailExpenseIngestResponse(
                "pending_confirmation",
                "Transaction sent to Telegram for confirmation.",
                pendingExpense.getAmount(),
                pendingExpense.getCategory() == null ? null : pendingExpense.getCategory().displayName(),
                pendingExpense.getMerchant(),
                pendingExpense.getExpenseDate()
        );
    }

    static EmailExpenseIngestResponse duplicate(String message) {
        return new EmailExpenseIngestResponse("duplicate", message, null, null, null, null);
    }

    static EmailExpenseIngestResponse rejected(String message) {
        return new EmailExpenseIngestResponse("rejected", message, null, null, null, null);
    }
}
