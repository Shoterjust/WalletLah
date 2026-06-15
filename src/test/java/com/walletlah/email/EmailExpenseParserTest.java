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
import java.util.List;
import org.junit.jupiter.api.Test;

class EmailExpenseParserTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-06-05T04:00:00Z"), ZoneId.of("Asia/Singapore"));
    private final EmailExpenseParser parser = new EmailExpenseParser(List.of(
            new DbsEmailExpenseParser(clock),
            new OcbcEmailExpenseParser(clock),
            new UobEmailExpenseParser(clock),
            new TrustEmailExpenseParser(clock),
            new YouTripEmailExpenseParser(clock),
            new RevolutEmailExpenseParser(clock),
            new GenericEmailExpenseParser(clock)
    ));

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
        assertThat(expense.merchant()).isEqualTo("MCDONALD'S");
        assertThat(expense.sourceProvider()).isEqualTo("GENERIC");
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
    void usesDbsParserWhenSenderOrBodyMatches() {
        ParsedEmailExpense expense = parser.parse(new EmailExpenseIngestRequest(
                "me@example.com",
                "alerts@dbs.com",
                "DBS Card Transaction Alert",
                "You have charged SGD 5.60 at Koufu on 05 Jun 2026. Available balance: SGD 900.00",
                "message-4",
                null
        ));

        assertThat(expense.amount()).isEqualByComparingTo(new BigDecimal("5.60"));
        assertThat(expense.merchant()).isEqualTo("Koufu");
        assertThat(expense.category()).isEqualTo(ExpenseCategory.FOOD);
        assertThat(expense.sourceProvider()).isEqualTo("DBS");
        assertThat(expense.expenseDate()).isEqualTo(LocalDate.of(2026, 6, 5));
    }

    @Test
    void usesOcbcParserWhenTemplateMatches() {
        ParsedEmailExpense expense = parser.parse(new EmailExpenseIngestRequest(
                "me@example.com",
                "alerts@ocbc.com",
                "OCBC Card Transaction",
                "A card transaction of S$18.90 was made at FAIRPRICE on 06/06/2026.",
                "message-5",
                null
        ));

        assertThat(expense.amount()).isEqualByComparingTo(new BigDecimal("18.90"));
        assertThat(expense.sourceProvider()).isEqualTo("OCBC");
        assertThat(expense.category()).isEqualTo(ExpenseCategory.SHOPPING);
        assertThat(expense.expenseDate()).isEqualTo(LocalDate.of(2026, 6, 6));
    }

    @Test
    void usesUobParserWhenTemplateMatches() {
        ParsedEmailExpense expense = parser.parse(new EmailExpenseIngestRequest(
                "me@example.com",
                "unialert@uobgroup.com",
                "UOB UniAlert",
                "S$14.20 spent on your UOB card at Starbucks on 7 June 2026.",
                "message-6",
                null
        ));

        assertThat(expense.amount()).isEqualByComparingTo(new BigDecimal("14.20"));
        assertThat(expense.merchant()).isEqualTo("Starbucks");
        assertThat(expense.sourceProvider()).isEqualTo("UOB");
        assertThat(expense.category()).isEqualTo(ExpenseCategory.FOOD);
        assertThat(expense.expenseDate()).isEqualTo(LocalDate.of(2026, 6, 7));
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
