package com.walletlah.receipt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.walletlah.expense.AddExpenseRequest;
import com.walletlah.expense.Expense;
import com.walletlah.expense.ExpenseCategory;
import com.walletlah.expense.ExpenseService;
import com.walletlah.expense.ExpenseSource;
import com.walletlah.user.WalletUser;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PendingExpenseServiceTest {

    private final PendingExpenseRepository repository = org.mockito.Mockito.mock(PendingExpenseRepository.class);
    private final ExpenseService expenseService = org.mockito.Mockito.mock(ExpenseService.class);
    private final PendingExpenseService service = new PendingExpenseService(
            repository,
            expenseService,
            new ReceiptScanProperties(true, "ap-southeast-1", 5_000_000, new BigDecimal("70"), 30, 20),
            Clock.fixed(Instant.parse("2026-06-05T04:00:00Z"), ZoneId.of("Asia/Singapore"))
    );

    @Test
    void confirmsPendingReceiptIntoExpense() {
        WalletUser user = new WalletUser(123L, 456L, "justin", "Justin");
        PendingExpense pending = new PendingExpense(123L, new ReceiptScanResult(
                "Koufu",
                new BigDecimal("6.80"),
                LocalDate.of(2026, 6, 5),
                ExpenseCategory.FOOD,
                new BigDecimal("98.00"),
                "Koufu S$6.80"
        ), "telegram-file-id");
        pending.prePersist();

        when(repository.findFirstByTelegramUserIdAndStatusOrderByCreatedAtDesc(123L, PendingExpenseStatus.PENDING_CONFIRMATION))
                .thenReturn(Optional.of(pending));
        when(expenseService.addReceiptScan(eq(user), any(AddExpenseRequest.class), eq("Koufu"), eq("telegram-file-id")))
                .thenReturn(new Expense(user, new BigDecimal("6.80"), ExpenseCategory.FOOD, "Receipt: Koufu", LocalDate.of(2026, 6, 5)));

        Expense expense = service.confirm(user);

        assertThat(expense.getAmount()).isEqualByComparingTo("6.80");
        assertThat(pending.getStatus()).isEqualTo(PendingExpenseStatus.CONFIRMED);

        ArgumentCaptor<AddExpenseRequest> requestCaptor = ArgumentCaptor.forClass(AddExpenseRequest.class);
        verify(expenseService).addReceiptScan(eq(user), requestCaptor.capture(), eq("Koufu"), eq("telegram-file-id"));
        assertThat(requestCaptor.getValue().category()).isEqualTo(ExpenseCategory.FOOD);
        assertThat(requestCaptor.getValue().description()).isEqualTo("Receipt: Koufu");
    }

    @Test
    void confirmsPendingEmailIntoEmailExpense() {
        WalletUser user = new WalletUser(123L, 456L, "justin", "Justin");
        PendingExpense pending = new PendingExpense(
                123L,
                "Koufu",
                new BigDecimal("6.80"),
                ExpenseCategory.FOOD,
                LocalDate.of(2026, 6, 5),
                "DBS",
                "message-1",
                "alerts@dbs.com",
                "DBS transaction",
                "raw text"
        );
        pending.prePersist();
        Expense savedExpense = new Expense(
                user,
                new BigDecimal("6.80"),
                ExpenseCategory.FOOD,
                "Email: Koufu",
                LocalDate.of(2026, 6, 5),
                "Koufu",
                ExpenseSource.EMAIL_INGEST,
                null,
                null
        );

        when(repository.findFirstByTelegramUserIdAndStatusOrderByCreatedAtDesc(123L, PendingExpenseStatus.PENDING_CONFIRMATION))
                .thenReturn(Optional.of(pending));
        when(expenseService.addEmailIngest(eq(user), any(AddExpenseRequest.class), eq("Koufu")))
                .thenReturn(savedExpense);

        Expense expense = service.confirm(user);

        assertThat(expense.getSource()).isEqualTo(ExpenseSource.EMAIL_INGEST);
        assertThat(pending.getStatus()).isEqualTo(PendingExpenseStatus.CONFIRMED);

        ArgumentCaptor<AddExpenseRequest> requestCaptor = ArgumentCaptor.forClass(AddExpenseRequest.class);
        verify(expenseService).addEmailIngest(eq(user), requestCaptor.capture(), eq("Koufu"));
        assertThat(requestCaptor.getValue().description()).isEqualTo("Email: Koufu");
    }
}
