package com.walletlah.expense;

import com.walletlah.common.MoneyUtils;
import com.walletlah.common.UserFacingException;
import com.walletlah.user.WalletUser;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;

    public ExpenseService(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    @Transactional
    public Expense add(WalletUser user, AddExpenseRequest request) {
        Expense expense = new Expense(
                user,
                MoneyUtils.money(request.amount()),
                request.category(),
                request.description(),
                request.expenseDate()
        );
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
                receiptImageFileId
        );
        return expenseRepository.save(expense);
    }

    @Transactional(readOnly = true)
    public List<RecentExpenseView> recent(WalletUser user, int limit) {
        return expenseRepository.findTop5ByUserOrderByExpenseDateDescCreatedAtDescIdDesc(user)
                .stream()
                .limit(limit)
                .map(RecentExpenseView::from)
                .toList();
    }

    @Transactional
    public Expense deleteLatest(WalletUser user) {
        Expense expense = expenseRepository.findFirstByUserOrderByCreatedAtDescIdDesc(user)
                .orElseThrow(() -> new UserFacingException("No expenses to delete yet."));
        expenseRepository.delete(expense);
        return expense;
    }
}
