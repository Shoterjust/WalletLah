package com.walletlah.email;

import com.walletlah.bot.TelegramBotService;
import com.walletlah.bot.TelegramResponseFormatter;
import com.walletlah.common.UserFacingException;
import com.walletlah.receipt.PendingExpense;
import com.walletlah.receipt.PendingExpenseService;
import com.walletlah.user.UserService;
import com.walletlah.user.WalletUser;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class EmailExpenseIngestService {

    private final UserService userService;
    private final PendingExpenseService pendingExpenseService;
    private final EmailExpenseParser parser;
    private final EmailExpenseIngestRepository ingestRepository;
    private final Optional<TelegramBotService> telegramBotService;
    private final TelegramResponseFormatter formatter;

    public EmailExpenseIngestService(
            UserService userService,
            PendingExpenseService pendingExpenseService,
            EmailExpenseParser parser,
            EmailExpenseIngestRepository ingestRepository,
            Optional<TelegramBotService> telegramBotService,
            TelegramResponseFormatter formatter
    ) {
        this.userService = userService;
        this.pendingExpenseService = pendingExpenseService;
        this.parser = parser;
        this.ingestRepository = ingestRepository;
        this.telegramBotService = telegramBotService;
        this.formatter = formatter;
    }

    @Transactional
    public EmailExpenseIngestResult ingest(EmailExpenseIngestRequest request) {
        WalletUser user = userService.walletUserWithEmail(request.recipientEmail())
                .orElseThrow(() -> new UserFacingException("No WalletLah user is linked to " + request.recipientEmail() + ". Send /email " + request.recipientEmail() + " in Telegram first."));

        String sourceMessageId = normalizeOptional(request.messageId(), 500);
        if (StringUtils.hasText(sourceMessageId)
                && ingestRepository.existsByUserAndSourceMessageId(user, sourceMessageId)) {
            return EmailExpenseIngestResult.alreadyLogged();
        }

        ParsedEmailExpense parsed = parser.parse(request);
        PendingExpense pendingExpense = pendingExpenseService.createEmail(
                user.getTelegramUserId(),
                parsed.amount(),
                parsed.category(),
                parsed.merchant(),
                parsed.expenseDate(),
                parsed.sourceProvider(),
                sourceMessageId,
                normalizeOptional(request.sender(), 500),
                normalizeOptional(request.subject(), 1000),
                normalizeOptional(parsed.rawText(), 5000)
        );

        ingestRepository.save(new EmailExpenseIngest(
                user,
                null,
                sourceMessageId,
                normalizeOptional(request.sender(), 500),
                normalizeOptional(request.subject(), 1000),
                normalizeOptional(parsed.sourceProvider(), 50)
        ));

        telegramBotService.ifPresent(bot -> bot.send(user.getTelegramChatId(), formatter.emailExpensePending(pendingExpense)));

        return EmailExpenseIngestResult.pending(pendingExpense);
    }

    private String normalizeOptional(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }
}
