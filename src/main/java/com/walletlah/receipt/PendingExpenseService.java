package com.walletlah.receipt;

import com.walletlah.common.MoneyUtils;
import com.walletlah.common.UserFacingException;
import com.walletlah.expense.AddExpenseRequest;
import com.walletlah.expense.Expense;
import com.walletlah.expense.ExpenseCategory;
import com.walletlah.expense.ExpenseService;
import com.walletlah.user.WalletUser;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PendingExpenseService {

    private final PendingExpenseRepository pendingExpenseRepository;
    private final ExpenseService expenseService;
    private final ReceiptScanProperties properties;
    private final Clock clock;

    public PendingExpenseService(
            PendingExpenseRepository pendingExpenseRepository,
            ExpenseService expenseService,
            ReceiptScanProperties properties,
            Clock clock
    ) {
        this.pendingExpenseRepository = pendingExpenseRepository;
        this.expenseService = expenseService;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public PendingExpense create(Long telegramUserId, ReceiptScanResult result, String receiptImageFileId) {
        return pendingExpenseRepository.save(new PendingExpense(telegramUserId, result, receiptImageFileId));
    }

    @Transactional
    public PendingExpense createEmail(
            Long telegramUserId,
            BigDecimal amount,
            ExpenseCategory category,
            String merchant,
            LocalDate expenseDate,
            String sourceProvider,
            String sourceMessageId,
            String sender,
            String subject,
            String rawText
    ) {
        return pendingExpenseRepository.save(new PendingExpense(
                telegramUserId,
                merchant,
                amount,
                category,
                expenseDate,
                sourceProvider,
                sourceMessageId,
                sender,
                subject,
                rawText
        ));
    }

    @Transactional
    public Optional<PendingExpense> activePending(Long telegramUserId) {
        Optional<PendingExpense> pending = pendingExpenseRepository.findFirstByTelegramUserIdAndStatusOrderByCreatedAtDesc(
                telegramUserId,
                PendingExpenseStatus.PENDING_CONFIRMATION
        );
        pending.ifPresent(this::expireIfNeeded);
        return pending.filter(item -> item.getStatus() == PendingExpenseStatus.PENDING_CONFIRMATION);
    }

    public boolean looksLikePendingReply(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        String normalized = text.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("yes")
                || normalized.equals("y")
                || normalized.equals("no")
                || normalized.equals("n")
                || normalized.startsWith("amount ")
                || normalized.startsWith("category ")
                || normalized.startsWith("date ")
                || normalized.startsWith("merchant ");
    }

    @Transactional
    public Expense confirm(WalletUser user) {
        PendingExpense pending = requireActivePending(user.getTelegramUserId(), "No pending expense to save.");

        if (pending.getAmount() == null) {
            throw new UserFacingException("I still need the amount before saving. Reply like: amount 7.20");
        }
        if (pending.getCategory() == null) {
            throw new UserFacingException("I still need the category before saving. Reply like: category food");
        }
        if (pending.getExpenseDate() == null) {
            throw new UserFacingException("I still need the date before saving. Reply like: date 2026-06-05");
        }

        AddExpenseRequest request = new AddExpenseRequest(
                pending.getAmount(),
                pending.getCategory(),
                descriptionFor(pending),
                pending.getExpenseDate()
        );
        Expense expense = pending.getSource() == PendingExpenseSource.EMAIL_INGEST
                ? expenseService.addEmailIngest(user, request, pending.getMerchant())
                : expenseService.addReceiptScan(user, request, pending.getMerchant(), pending.getReceiptImageFileId());
        pending.markConfirmed();
        return expense;
    }

    @Transactional
    public PendingExpense cancel(Long telegramUserId) {
        PendingExpense pending = requireActivePending(telegramUserId, "No pending expense to cancel.");
        pending.markCancelled();
        return pending;
    }

    @Transactional
    public PendingExpense edit(Long telegramUserId, String text) {
        PendingExpense pending = requireActivePending(telegramUserId, "No pending expense to edit.");
        String[] parts = text.trim().split("\\s+", 2);
        if (parts.length < 2 || !StringUtils.hasText(parts[1])) {
            throw new UserFacingException("Edit using: amount 7.20, category food, date 2026-06-05, or merchant Koufu");
        }

        String field = parts[0].toLowerCase(Locale.ROOT);
        String value = parts[1].trim();
        switch (field) {
            case "amount" -> pending.updateAmount(parseAmount(value));
            case "category" -> pending.updateCategory(ExpenseCategory.from(value)
                    .orElseThrow(() -> new UserFacingException("Unknown category. Try category food or use /categories.")));
            case "date" -> pending.updateExpenseDate(parseDate(value));
            case "merchant" -> pending.updateMerchant(limit(value, 255));
            default -> throw new UserFacingException("Edit using: amount 7.20, category food, date 2026-06-05, or merchant Koufu");
        }
        return pending;
    }

    private PendingExpense requireActivePending(Long telegramUserId, String missingMessage) {
        PendingExpense pending = pendingExpenseRepository.findFirstByTelegramUserIdAndStatusOrderByCreatedAtDesc(
                telegramUserId,
                PendingExpenseStatus.PENDING_CONFIRMATION
        ).orElseThrow(() -> new UserFacingException(missingMessage));
        expireIfNeeded(pending);
        if (pending.getStatus() == PendingExpenseStatus.EXPIRED) {
            throw new UserFacingException("That pending expense expired. Please send it again.");
        }
        return pending;
    }

    private String descriptionFor(PendingExpense pending) {
        if (pending.getSource() == PendingExpenseSource.EMAIL_INGEST) {
            return StringUtils.hasText(pending.getMerchant())
                    ? "Email: " + pending.getMerchant()
                    : "Email transaction";
        }
        return StringUtils.hasText(pending.getMerchant())
                ? "Receipt: " + pending.getMerchant()
                : "Receipt scan";
    }

    private void expireIfNeeded(PendingExpense pending) {
        Instant expiresAt = pending.getCreatedAt().plus(Duration.ofMinutes(properties.pendingExpiryMinutes()));
        if (Instant.now(clock).isAfter(expiresAt)) {
            pending.markExpired();
        }
    }

    private BigDecimal parseAmount(String value) {
        try {
            BigDecimal amount = new BigDecimal(value.replace("S$", "").replace("$", "").trim());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new UserFacingException("Amount must be more than zero.");
            }
            return MoneyUtils.money(amount);
        } catch (NumberFormatException e) {
            throw new UserFacingException("I could not read that amount. Try: amount 7.20");
        }
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new UserFacingException("I could not read that date. Try: date 2026-06-05");
        }
    }

    private String limit(String value, int maxLength) {
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
