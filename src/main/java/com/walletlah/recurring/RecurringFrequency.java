package com.walletlah.recurring;

import com.walletlah.common.UserFacingException;
import java.time.LocalDate;
import java.util.Locale;

public enum RecurringFrequency {
    DAILY,
    WEEKLY,
    MONTHLY;

    public LocalDate nextAfter(LocalDate date) {
        return switch (this) {
            case DAILY -> date.plusDays(1);
            case WEEKLY -> date.plusWeeks(1);
            case MONTHLY -> date.plusMonths(1);
        };
    }

    public static RecurringFrequency parse(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new UserFacingException("Frequency must be daily, weekly, or monthly.");
        }
        return switch (rawValue.trim().toLowerCase(Locale.ROOT)) {
            case "daily", "day" -> DAILY;
            case "weekly", "week" -> WEEKLY;
            case "monthly", "month" -> MONTHLY;
            default -> throw new UserFacingException("Frequency must be daily, weekly, or monthly.");
        };
    }
}
