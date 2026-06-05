package com.walletlah.email;

import com.walletlah.common.UserFacingException;
import com.walletlah.expense.AddExpenseRequest;
import com.walletlah.expense.Expense;
import com.walletlah.expense.ExpenseService;
import com.walletlah.user.UserService;
import com.walletlah.user.WalletUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class EmailExpenseIngestService {

    private final UserService userService;
    private final ExpenseService expenseService;
    private final EmailExpenseParser parser;
    private final EmailExpenseIngestRepository ingestRepository;

    public EmailExpenseIngestService(
            UserService userService,
            ExpenseService expenseService,
            EmailExpenseParser parser,
            EmailExpenseIngestRepository ingestRepository
    ) {
        this.userService = userService;
        this.expenseService = expenseService;
        this.parser = parser;
        this.ingestRepository = ingestRepository;
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
        Expense expense = expenseService.add(user, new AddExpenseRequest(
                parsed.amount(),
                parsed.category(),
                parsed.description(),
                parsed.expenseDate()
        ));

        ingestRepository.save(new EmailExpenseIngest(
                user,
                expense,
                sourceMessageId,
                normalizeOptional(request.sender(), 500),
                normalizeOptional(request.subject(), 1000)
        ));

        return EmailExpenseIngestResult.logged(expense);
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
