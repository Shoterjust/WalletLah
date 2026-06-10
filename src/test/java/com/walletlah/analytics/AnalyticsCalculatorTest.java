package com.walletlah.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class AnalyticsCalculatorTest {

    @Test
    void calculatesDaysLeftIncludingToday() {
        assertThat(AnalyticsCalculator.daysLeftInMonth(LocalDate.of(2026, 6, 1))).isEqualTo(30);
        assertThat(AnalyticsCalculator.daysLeftInMonth(LocalDate.of(2026, 6, 30))).isEqualTo(1);
    }

    @Test
    void calculatesDaysElapsedInMonth() {
        assertThat(AnalyticsCalculator.daysElapsedInMonth(LocalDate.of(2026, 6, 1))).isEqualTo(1);
        assertThat(AnalyticsCalculator.daysElapsedInMonth(LocalDate.of(2026, 6, 10))).isEqualTo(10);
    }

    @Test
    void calculatesAverageDailySpend() {
        BigDecimal averageDailySpend = AnalyticsCalculator.averageDailySpend(new BigDecimal("155.00"), 10);

        assertThat(averageDailySpend).isEqualByComparingTo(new BigDecimal("15.50"));
    }

    @Test
    void calculatesSafeDailySpend() {
        BigDecimal safeDailySpend = AnalyticsCalculator.safeDailySpend(new BigDecimal("300.00"), 30);

        assertThat(safeDailySpend).isEqualByComparingTo(new BigDecimal("10.00"));
    }

    @Test
    void neverReturnsNegativeSafeDailySpend() {
        BigDecimal safeDailySpend = AnalyticsCalculator.safeDailySpend(new BigDecimal("-15.00"), 5);

        assertThat(safeDailySpend).isEqualByComparingTo(new BigDecimal("0.00"));
    }

    @Test
    void calculatesCategoryPercentage() {
        BigDecimal percentage = AnalyticsCalculator.percentage(new BigDecimal("25.00"), new BigDecimal("100.00"));

        assertThat(percentage).isEqualByComparingTo(new BigDecimal("25.0"));
    }
}
