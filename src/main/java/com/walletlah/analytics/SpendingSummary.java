package com.walletlah.analytics;

import java.math.BigDecimal;

public record SpendingSummary(
        String monthLabel,
        BigDecimal totalSpent,
        BigDecimal monthlyBudget,
        BigDecimal remainingBudget,
        int daysLeftInMonth,
        BigDecimal safeDailySpend,
        int daysElapsedInMonth,
        BigDecimal averageDailySpend,
        BigDecimal budgetUsedPercentage
) {
}
