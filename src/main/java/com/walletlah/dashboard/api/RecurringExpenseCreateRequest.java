package com.walletlah.dashboard.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurringExpenseCreateRequest(
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotBlank String category,
        @NotBlank String description,
        String merchant,
        @NotBlank String frequency,
        @NotNull LocalDate nextRunDate
) {
}
