package com.walletlah.dashboard.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseCreateRequest(
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotBlank String category,
        String description,
        @NotNull LocalDate expenseDate,
        String merchant
) {
}
