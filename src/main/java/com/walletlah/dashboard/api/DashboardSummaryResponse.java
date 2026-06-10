package com.walletlah.dashboard.api;

import com.walletlah.analytics.SpendingSummary;
import java.math.BigDecimal;
import java.util.List;

public record DashboardSummaryResponse(
        String monthLabel,
        BigDecimal totalSpent,
        BigDecimal monthlyBudget,
        BigDecimal remainingBudget,
        int daysLeftInMonth,
        BigDecimal safeDailySpend,
        int daysElapsedInMonth,
        BigDecimal averageDailySpend,
        BigDecimal budgetUsedPercentage,
        List<CategoryBreakdownResponse> categories
) {

    public static DashboardSummaryResponse from(
            SpendingSummary summary,
            List<CategoryBreakdownResponse> categories
    ) {
        return new DashboardSummaryResponse(
                summary.monthLabel(),
                summary.totalSpent(),
                summary.monthlyBudget(),
                summary.remainingBudget(),
                summary.daysLeftInMonth(),
                summary.safeDailySpend(),
                summary.daysElapsedInMonth(),
                summary.averageDailySpend(),
                summary.budgetUsedPercentage(),
                categories
        );
    }
}
