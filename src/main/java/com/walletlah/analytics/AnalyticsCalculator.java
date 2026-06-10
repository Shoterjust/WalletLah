package com.walletlah.analytics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class AnalyticsCalculator {

    private AnalyticsCalculator() {
    }

    public static BigDecimal remaining(BigDecimal budget, BigDecimal totalSpent) {
        return budget.subtract(totalSpent).setScale(2, RoundingMode.HALF_UP);
    }

    public static int daysLeftInMonth(LocalDate today) {
        return Math.toIntExact(ChronoUnit.DAYS.between(today, today.withDayOfMonth(today.lengthOfMonth())) + 1);
    }

    public static int daysElapsedInMonth(LocalDate today) {
        return today.getDayOfMonth();
    }

    public static BigDecimal averageDailySpend(BigDecimal totalSpent, int daysElapsedInMonth) {
        if (daysElapsedInMonth <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return totalSpent.divide(BigDecimal.valueOf(daysElapsedInMonth), 2, RoundingMode.HALF_UP);
    }

    public static BigDecimal safeDailySpend(BigDecimal remainingBudget, int daysLeftInMonth) {
        if (remainingBudget.compareTo(BigDecimal.ZERO) <= 0 || daysLeftInMonth <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return remainingBudget.divide(BigDecimal.valueOf(daysLeftInMonth), 2, RoundingMode.HALF_UP);
    }

    public static BigDecimal percentage(BigDecimal categoryTotal, BigDecimal totalSpent) {
        if (totalSpent.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return categoryTotal
                .multiply(BigDecimal.valueOf(100))
                .divide(totalSpent, 1, RoundingMode.HALF_UP);
    }
}
