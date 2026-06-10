package com.walletlah.recurring;

import com.walletlah.common.MoneyUtils;
import com.walletlah.common.UserFacingException;
import com.walletlah.expense.AddExpenseRequest;
import com.walletlah.expense.ExpenseService;
import com.walletlah.user.WalletUser;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecurringExpenseService {

    private static final int MAX_GENERATIONS_PER_RULE = 24;

    private final RecurringExpenseRepository recurringExpenseRepository;
    private final ExpenseService expenseService;
    private final Clock clock;

    public RecurringExpenseService(
            RecurringExpenseRepository recurringExpenseRepository,
            ExpenseService expenseService,
            Clock clock
    ) {
        this.recurringExpenseRepository = recurringExpenseRepository;
        this.expenseService = expenseService;
        this.clock = clock;
    }

    @Transactional
    public RecurringExpense add(WalletUser user, AddRecurringExpenseRequest request) {
        RecurringExpense recurringExpense = new RecurringExpense(
                user,
                MoneyUtils.money(request.amount()),
                request.category(),
                request.description(),
                request.merchant(),
                request.frequency(),
                request.nextRunDate()
        );
        return recurringExpenseRepository.save(recurringExpense);
    }

    @Transactional(readOnly = true)
    public List<RecurringExpense> listActive(WalletUser user) {
        return recurringExpenseRepository.findByUserAndActiveTrueOrderByNextRunDateAscIdAsc(user);
    }

    @Transactional
    public RecurringExpense cancel(WalletUser user, Long id) {
        RecurringExpense recurringExpense = recurringExpenseRepository.findByIdAndUserAndActiveTrue(id, user)
                .orElseThrow(() -> new UserFacingException("I could not find active recurring expense #" + id + "."));
        recurringExpense.deactivate();
        return recurringExpense;
    }

    @Transactional
    public int generateDueExpenses() {
        LocalDate today = LocalDate.now(clock);
        List<RecurringExpense> dueRules = recurringExpenseRepository
                .findByActiveTrueAndNextRunDateLessThanEqualOrderByNextRunDateAscIdAsc(today);
        int generated = 0;
        for (RecurringExpense recurringExpense : dueRules) {
            generated += generateForRule(recurringExpense, today);
        }
        return generated;
    }

    private int generateForRule(RecurringExpense recurringExpense, LocalDate today) {
        int generated = 0;
        while (!recurringExpense.getNextRunDate().isAfter(today) && generated < MAX_GENERATIONS_PER_RULE) {
            expenseService.addRecurringGenerated(
                    recurringExpense.getUser(),
                    new AddExpenseRequest(
                            recurringExpense.getAmount(),
                            recurringExpense.getCategory(),
                            recurringExpense.getDescription(),
                            recurringExpense.getNextRunDate()
                    ),
                    recurringExpense.getMerchant(),
                    recurringExpense.getId()
            );
            recurringExpense.advanceNextRunDate();
            generated++;
        }
        return generated;
    }
}
