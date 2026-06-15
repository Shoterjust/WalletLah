package com.walletlah.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.walletlah.bot.TelegramBotService;
import com.walletlah.bot.TelegramResponseFormatter;
import com.walletlah.expense.ExpenseCategory;
import com.walletlah.receipt.PendingExpense;
import com.walletlah.receipt.PendingExpenseService;
import com.walletlah.user.UserService;
import com.walletlah.user.WalletUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EmailExpenseIngestServiceTest {

    private final UserService userService = org.mockito.Mockito.mock(UserService.class);
    private final PendingExpenseService pendingExpenseService = org.mockito.Mockito.mock(PendingExpenseService.class);
    private final EmailExpenseParser parser = org.mockito.Mockito.mock(EmailExpenseParser.class);
    private final EmailExpenseIngestRepository ingestRepository = org.mockito.Mockito.mock(EmailExpenseIngestRepository.class);
    private final TelegramBotService telegramBotService = org.mockito.Mockito.mock(TelegramBotService.class);
    private final TelegramResponseFormatter formatter = org.mockito.Mockito.mock(TelegramResponseFormatter.class);
    private final EmailExpenseIngestService service = new EmailExpenseIngestService(
            userService,
            pendingExpenseService,
            parser,
            ingestRepository,
            Optional.of(telegramBotService),
            formatter
    );

    @Test
    void createsPendingExpenseAndSendsTelegramConfirmation() {
        WalletUser user = new WalletUser(123L, 456L, "justin", "Justin");
        EmailExpenseIngestRequest request = new EmailExpenseIngestRequest(
                "me@example.com",
                "alerts@dbs.com",
                "DBS transaction",
                "You have charged SGD 5.60 at Koufu",
                "message-1",
                null
        );
        ParsedEmailExpense parsed = new ParsedEmailExpense(
                new BigDecimal("5.60"),
                ExpenseCategory.FOOD,
                "Koufu",
                LocalDate.of(2026, 6, 5),
                "DBS",
                "raw text"
        );
        PendingExpense pendingExpense = new PendingExpense(
                123L,
                "Koufu",
                new BigDecimal("5.60"),
                ExpenseCategory.FOOD,
                LocalDate.of(2026, 6, 5),
                "DBS",
                "message-1",
                "alerts@dbs.com",
                "DBS transaction",
                "raw text"
        );

        when(userService.walletUserWithEmail("me@example.com")).thenReturn(Optional.of(user));
        when(ingestRepository.existsByUserAndSourceMessageId(user, "message-1")).thenReturn(false);
        when(parser.parse(request)).thenReturn(parsed);
        when(pendingExpenseService.createEmail(
                eq(123L),
                eq(new BigDecimal("5.60")),
                eq(ExpenseCategory.FOOD),
                eq("Koufu"),
                eq(LocalDate.of(2026, 6, 5)),
                eq("DBS"),
                eq("message-1"),
                eq("alerts@dbs.com"),
                eq("DBS transaction"),
                eq("raw text")
        )).thenReturn(pendingExpense);
        when(formatter.emailExpensePending(pendingExpense)).thenReturn("confirm this");

        EmailExpenseIngestResult result = service.ingest(request);

        assertThat(result.duplicate()).isFalse();
        assertThat(result.pendingExpense()).isSameAs(pendingExpense);
        verify(telegramBotService).send(456L, "confirm this");

        ArgumentCaptor<EmailExpenseIngest> ingestCaptor = ArgumentCaptor.forClass(EmailExpenseIngest.class);
        verify(ingestRepository).save(ingestCaptor.capture());
    }

    @Test
    void duplicateMessageDoesNotCreateAnotherPendingExpenseOrNotification() {
        WalletUser user = new WalletUser(123L, 456L, "justin", "Justin");
        EmailExpenseIngestRequest request = new EmailExpenseIngestRequest(
                "me@example.com",
                "alerts@dbs.com",
                "DBS transaction",
                "You have charged SGD 5.60 at Koufu",
                "message-1",
                null
        );

        when(userService.walletUserWithEmail("me@example.com")).thenReturn(Optional.of(user));
        when(ingestRepository.existsByUserAndSourceMessageId(user, "message-1")).thenReturn(true);

        EmailExpenseIngestResult result = service.ingest(request);

        assertThat(result.duplicate()).isTrue();
        verify(parser, never()).parse(any());
        verify(pendingExpenseService, never()).createEmail(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(telegramBotService, never()).send(any(), any());
    }
}
