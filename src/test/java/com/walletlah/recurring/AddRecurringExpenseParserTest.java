package com.walletlah.recurring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.walletlah.common.UserFacingException;
import com.walletlah.expense.ExpenseCategory;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class AddRecurringExpenseParserTest {

    private final AddRecurringExpenseParser parser = new AddRecurringExpenseParser(
            Clock.fixed(Instant.parse("2026-06-10T04:00:00Z"), ZoneId.of("Asia/Singapore"))
    );

    @Test
    void parsesMonthlyRecurringExpenseStartingToday() {
        AddRecurringExpenseRequest request = parser.parse("14.99 subscriptions Spotify monthly");

        assertThat(request.amount()).isEqualByComparingTo(new BigDecimal("14.99"));
        assertThat(request.category()).isEqualTo(ExpenseCategory.SUBSCRIPTIONS);
        assertThat(request.description()).isEqualTo("Spotify");
        assertThat(request.frequency()).isEqualTo(RecurringFrequency.MONTHLY);
        assertThat(request.nextRunDate()).isEqualTo(LocalDate.of(2026, 6, 10));
    }

    @Test
    void parsesOptionalNextRunDate() {
        AddRecurringExpenseRequest request = parser.parse("80 transport concession monthly 2026-07-01");

        assertThat(request.amount()).isEqualByComparingTo(new BigDecimal("80"));
        assertThat(request.category()).isEqualTo(ExpenseCategory.TRANSPORT);
        assertThat(request.description()).isEqualTo("concession");
        assertThat(request.frequency()).isEqualTo(RecurringFrequency.MONTHLY);
        assertThat(request.nextRunDate()).isEqualTo(LocalDate.of(2026, 7, 1));
    }

    @Test
    void rejectsInvalidFrequency() {
        assertThatThrownBy(() -> parser.parse("14.99 subscriptions Spotify yearly"))
                .isInstanceOf(UserFacingException.class);
    }
}
