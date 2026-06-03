package com.walletlah.analytics;

import com.walletlah.expense.ExpenseCategory;
import java.math.BigDecimal;

public record CategoryBreakdownItem(
        ExpenseCategory category,
        BigDecimal total,
        BigDecimal percentage
) {
}
