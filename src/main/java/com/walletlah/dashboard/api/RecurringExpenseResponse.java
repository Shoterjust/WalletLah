package com.walletlah.dashboard.api;

import com.walletlah.recurring.RecurringExpense;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record RecurringExpenseResponse(
        Long id,
        BigDecimal amount,
        String category,
        String categoryDisplayName,
        String description,
        String merchant,
        String frequency,
        LocalDate nextRunDate,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {

    public static RecurringExpenseResponse from(RecurringExpense recurringExpense) {
        return new RecurringExpenseResponse(
                recurringExpense.getId(),
                recurringExpense.getAmount(),
                recurringExpense.getCategory().name(),
                recurringExpense.getCategory().displayName(),
                recurringExpense.getDescription(),
                recurringExpense.getMerchant(),
                recurringExpense.getFrequency().name(),
                recurringExpense.getNextRunDate(),
                recurringExpense.isActive(),
                recurringExpense.getCreatedAt(),
                recurringExpense.getUpdatedAt()
        );
    }
}
