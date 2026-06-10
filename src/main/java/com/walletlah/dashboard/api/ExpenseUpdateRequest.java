package com.walletlah.dashboard.api;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseUpdateRequest(
        @DecimalMin(value = "0.01") BigDecimal amount,
        String category,
        String description,
        LocalDate expenseDate,
        String merchant
) {
}
