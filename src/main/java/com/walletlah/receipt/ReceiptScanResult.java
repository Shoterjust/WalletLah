package com.walletlah.receipt;

import com.walletlah.expense.ExpenseCategory;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ReceiptScanResult(
        String merchant,
        BigDecimal amount,
        LocalDate expenseDate,
        ExpenseCategory category,
        BigDecimal confidence,
        String rawOcrText
) {
}
