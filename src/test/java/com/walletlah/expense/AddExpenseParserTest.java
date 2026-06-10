package com.walletlah.expense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.walletlah.common.UserFacingException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class AddExpenseParserTest {

    private final AddExpenseParser parser = new AddExpenseParser(
            Clock.fixed(Instant.parse("2026-06-01T04:00:00Z"), ZoneId.of("Asia/Singapore"))
    );

    @Test
    void parsesCommandBodyWithAmountFirst() {
        AddExpenseRequest request = parser.parse("5.50 food chicken rice");

        assertThat(request.amount()).isEqualByComparingTo(new BigDecimal("5.50"));
        assertThat(request.category()).isEqualTo(ExpenseCategory.FOOD);
        assertThat(request.description()).isEqualTo("chicken rice");
        assertThat(request.expenseDate()).isEqualTo(LocalDate.of(2026, 6, 1));
    }

    @Test
    void parsesCategoryFirstShortcut() {
        AddExpenseRequest request = parser.parse("transport 1.09 mrt to school");

        assertThat(request.amount()).isEqualByComparingTo(new BigDecimal("1.09"));
        assertThat(request.category()).isEqualTo(ExpenseCategory.TRANSPORT);
        assertThat(request.description()).isEqualTo("mrt to school");
    }

    @Test
    void defaultsUnknownCategoryToOthersWhenAmountIsClear() {
        AddExpenseRequest request = parser.parse("3.20 bubble tea");

        assertThat(request.amount()).isEqualByComparingTo(new BigDecimal("3.20"));
        assertThat(request.category()).isEqualTo(ExpenseCategory.OTHERS);
        assertThat(request.description()).isEqualTo("bubble tea");
    }

    @Test
    void rejectsInvalidExpenseText() {
        assertThatThrownBy(() -> parser.parse("food chicken rice"))
                .isInstanceOf(UserFacingException.class);
    }
}
