package com.walletlah.common;

import java.time.LocalDate;
import java.time.YearMonth;

public record MonthRange(LocalDate startInclusive, LocalDate endExclusive) {

    public static MonthRange from(YearMonth month) {
        return new MonthRange(month.atDay(1), month.plusMonths(1).atDay(1));
    }
}
