package com.walletlah.email;

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

class EmailExpenseParserTest {

    private final EmailExpenseParser parser = new EmailExpenseParser(
            Clock.fixed(Instant.parse("2026-06-05T04:00:00Z"), ZoneId.of("Asia/Singapore"))
    );

    @Test
    void parsesCardTransactionEmail() {
        ParsedEmailExpense expense = parser.parse(new EmailExpenseIngestRequest(
                "me@example.com",
                "alerts@bank.example",
                "Card transaction alert",
                "You have spent SGD 12.30 at MCDONALD'S on 05 Jun 2026. Available balance: SGD 800.00",
                "message-1",
                null
        ));

        assertThat(expense.amount()).isEqualByComparingTo(new BigDecimal("12.30"));
        assertThat(expense.category()).isEqualTo(ExpenseCategory.FOOD);
        assertThat(expense.description()).isEqualTo("Auto: MCDONALD'S");
        assertThat(expense.expenseDate()).isEqualTo(LocalDate.of(2026, 6, 5));
    }

    @Test
    void usesProvidedExpenseDate() {
        ParsedEmailExpense expense = parser.parse(new EmailExpenseIngestRequest(
                "me@example.com",
                "receipts@example.com",
                "Receipt from Grab",
                "Transaction amount: S$8.70 Merchant: Grab",
                "message-2",
                LocalDate.of(2026, 6, 4)
        ));

        assertThat(expense.amount()).isEqualByComparingTo(new BigDecimal("8.70"));
        assertThat(expense.category()).isEqualTo(ExpenseCategory.TRANSPORT);
        assertThat(expense.expenseDate()).isEqualTo(LocalDate.of(2026, 6, 4));
    }

    @Test
    void rejectsRefundsAndCardPayments() {
        assertThatThrownBy(() -> parser.parse(new EmailExpenseIngestRequest(
                "me@example.com",
                "alerts@bank.example",
                "Payment received",
                "Your credit card payment of SGD 100.00 has been received.",
                "message-3",
                null
        ))).isInstanceOf(UserFacingException.class);
    }
}
