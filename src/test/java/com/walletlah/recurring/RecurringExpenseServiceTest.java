package com.walletlah.recurring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.walletlah.expense.AddExpenseRequest;
import com.walletlah.expense.ExpenseCategory;
import com.walletlah.expense.ExpenseService;
import com.walletlah.user.WalletUser;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RecurringExpenseServiceTest {

    private final RecurringExpenseRepository repository = org.mockito.Mockito.mock(RecurringExpenseRepository.class);
    private final ExpenseService expenseService = org.mockito.Mockito.mock(ExpenseService.class);
    private final RecurringExpenseService service = new RecurringExpenseService(
            repository,
            expenseService,
            Clock.fixed(Instant.parse("2026-06-10T04:00:00Z"), ZoneId.of("Asia/Singapore"))
    );

    @Test
    void generatesDueRecurringExpenseAndAdvancesNextRunDate() {
        WalletUser user = new WalletUser(123L, 456L, "justin", "Justin");
        RecurringExpense recurringExpense = new RecurringExpense(
                user,
                new BigDecimal("14.99"),
                ExpenseCategory.SUBSCRIPTIONS,
                "Spotify",
                "Spotify",
                RecurringFrequency.MONTHLY,
                LocalDate.of(2026, 6, 1)
        );

        when(repository.findByActiveTrueAndNextRunDateLessThanEqualOrderByNextRunDateAscIdAsc(LocalDate.of(2026, 6, 10)))
                .thenReturn(List.of(recurringExpense));

        int generated = service.generateDueExpenses();

        assertThat(generated).isEqualTo(1);
        assertThat(recurringExpense.getNextRunDate()).isEqualTo(LocalDate.of(2026, 7, 1));

        ArgumentCaptor<AddExpenseRequest> requestCaptor = ArgumentCaptor.forClass(AddExpenseRequest.class);
        verify(expenseService).addRecurringGenerated(eq(user), requestCaptor.capture(), eq("Spotify"), any());
        assertThat(requestCaptor.getValue().amount()).isEqualByComparingTo("14.99");
        assertThat(requestCaptor.getValue().category()).isEqualTo(ExpenseCategory.SUBSCRIPTIONS);
        assertThat(requestCaptor.getValue().expenseDate()).isEqualTo(LocalDate.of(2026, 6, 1));
    }
}
