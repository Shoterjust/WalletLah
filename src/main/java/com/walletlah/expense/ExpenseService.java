package com.walletlah.expense;

import com.walletlah.common.MoneyUtils;
import com.walletlah.common.UserFacingException;
import com.walletlah.user.WalletUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;

    public ExpenseService(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    @Transactional
    public Expense add(WalletUser user, AddExpenseRequest request) {
        return addManual(
                user,
                request.amount(),
                request.category(),
                request.description(),
                request.expenseDate(),
                null
        );
    }

    @Transactional
    public Expense addManual(
            WalletUser user,
            BigDecimal amount,
            ExpenseCategory category,
            String description,
            LocalDate expenseDate,
            String merchant
    ) {
        Expense expense = new Expense(
                user,
                validateAmount(amount),
                category,
                limit(description, 255),
                expenseDate
        );
        if (StringUtils.hasText(merchant)) {
            expense.updateMerchant(limit(merchant, 255));
        }
        return expenseRepository.save(expense);
    }

    @Transactional
    public Expense addReceiptScan(
            WalletUser user,
            AddExpenseRequest request,
            String merchant,
            String receiptImageFileId
    ) {
        Expense expense = new Expense(
                user,
                MoneyUtils.money(request.amount()),
                request.category(),
                request.description(),
                request.expenseDate(),
                merchant,
                ExpenseSource.RECEIPT_SCAN,
                receiptImageFileId,
                null
        );
        return expenseRepository.save(expense);
    }

    @Transactional
    public Expense addRecurringGenerated(WalletUser user, AddExpenseRequest request, String merchant, Long recurringExpenseId) {
        Expense expense = new Expense(
                user,
                MoneyUtils.money(request.amount()),
                request.category(),
                request.description(),
                request.expenseDate(),
                merchant,
                ExpenseSource.RECURRING,
                null,
                recurringExpenseId
        );
        return expenseRepository.save(expense);
    }

    @Transactional(readOnly = true)
    public List<RecentExpenseView> recent(WalletUser user, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 20));
        return expenseRepository.findByUserOrderByExpenseDateDescCreatedAtDescIdDesc(user, PageRequest.of(0, safeLimit))
                .stream()
                .map(RecentExpenseView::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<Expense> dashboardPage(
            WalletUser user,
            LocalDate startInclusive,
            LocalDate endExclusive,
            ExpenseCategory category,
            ExpenseSource source,
            Pageable pageable
    ) {
        return expenseRepository.findDashboardPage(user, startInclusive, endExclusive, category, source, pageable);
    }

    @Transactional
    public Expense deleteLatest(WalletUser user) {
        Expense expense = expenseRepository.findFirstByUserOrderByCreatedAtDescIdDesc(user)
                .orElseThrow(() -> new UserFacingException("No expenses to delete yet."));
        expenseRepository.delete(expense);
        return expense;
    }

    @Transactional
    public Expense delete(WalletUser user, Long expenseId) {
        Expense expense = expenseRepository.findByIdAndUser(expenseId, user)
                .orElseThrow(() -> new UserFacingException("I could not find expense #" + expenseId + " for your account."));
        expenseRepository.delete(expense);
        return expense;
    }

    @Transactional
    public Expense edit(WalletUser user, Long expenseId, String field, String value) {
        Expense expense = expenseRepository.findByIdAndUser(expenseId, user)
                .orElseThrow(() -> new UserFacingException("I could not find expense #" + expenseId + " for your account."));
        applyEdit(expense, field, value);
        return expense;
    }

    @Transactional
    public Expense updateFromDashboard(
            WalletUser user,
            Long expenseId,
            BigDecimal amount,
            String category,
            String description,
            LocalDate expenseDate,
            String merchant
    ) {
        Expense expense = expenseRepository.findByIdAndUser(expenseId, user)
                .orElseThrow(() -> new UserFacingException("I could not find expense #" + expenseId + " for your account."));
        if (amount != null) {
            expense.updateAmount(validateAmount(amount));
        }
        if (StringUtils.hasText(category)) {
            expense.updateCategory(ExpenseCategory.from(category)
                    .orElseThrow(() -> new UserFacingException("Unknown category. Use a valid WalletLah category.")));
        }
        if (description != null) {
            expense.updateDescription(limit(description, 255));
        }
        if (expenseDate != null) {
            expense.updateExpenseDate(expenseDate);
        }
        if (merchant != null) {
            expense.updateMerchant(limit(merchant, 255));
        }
        return expense;
    }

    @Transactional
    public Expense editLatest(WalletUser user, String field, String value) {
        Expense expense = expenseRepository.findFirstByUserOrderByCreatedAtDescIdDesc(user)
                .orElseThrow(() -> new UserFacingException("No expenses to edit yet."));
        applyEdit(expense, field, value);
        return expense;
    }

    private void applyEdit(Expense expense, String rawField, String rawValue) {
        if (!StringUtils.hasText(rawField) || !StringUtils.hasText(rawValue)) {
            throw new UserFacingException("Edit using: /edit 12 amount 7.20");
        }

        String field = rawField.trim().toLowerCase(Locale.ROOT);
        String value = rawValue.trim();
        switch (field) {
            case "amount" -> expense.updateAmount(parseAmount(value));
            case "category" -> expense.updateCategory(ExpenseCategory.from(value)
                    .orElseThrow(() -> new UserFacingException("Unknown category. Use /categories to see valid categories.")));
            case "description", "desc" -> expense.updateDescription(limit(value, 255));
            case "date" -> expense.updateExpenseDate(parseDate(value));
            case "merchant" -> expense.updateMerchant(limit(value, 255));
            default -> throw new UserFacingException("Editable fields: amount, category, description, date, merchant.");
        }
    }

    private BigDecimal parseAmount(String value) {
        try {
            BigDecimal amount = new BigDecimal(value.replace("S$", "").replace("$", "").trim());
            return validateAmount(amount);
        } catch (NumberFormatException e) {
            throw new UserFacingException("I could not read that amount. Try: /edit_latest amount 7.20");
        }
    }

    private BigDecimal validateAmount(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new UserFacingException("Amount must be more than zero.");
        }
        return MoneyUtils.money(amount);
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (RuntimeException e) {
            throw new UserFacingException("I could not read that date. Try: /edit_latest date 2026-06-05");
        }
    }

    private String limit(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
