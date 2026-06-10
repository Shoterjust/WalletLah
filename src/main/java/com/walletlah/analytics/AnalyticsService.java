package com.walletlah.analytics;

import com.walletlah.budget.BudgetService;
import com.walletlah.common.MoneyUtils;
import com.walletlah.common.MonthRange;
import com.walletlah.expense.ExpenseCategory;
import com.walletlah.expense.ExpenseRepository;
import com.walletlah.user.WalletUser;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsService {

    private final ExpenseRepository expenseRepository;
    private final BudgetService budgetService;
    private final Clock clock;

    public AnalyticsService(ExpenseRepository expenseRepository, BudgetService budgetService, Clock clock) {
        this.expenseRepository = expenseRepository;
        this.budgetService = budgetService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public SpendingSummary monthlySummary(WalletUser user) {
        YearMonth currentMonth = YearMonth.now(clock);
        LocalDate today = LocalDate.now(clock);
        MonthRange range = MonthRange.from(currentMonth);
        BigDecimal totalSpent = MoneyUtils.money(expenseRepository.sumAmountForUserBetween(
                user,
                range.startInclusive(),
                range.endExclusive()
        ));
        int daysLeft = AnalyticsCalculator.daysLeftInMonth(today);
        int daysElapsed = AnalyticsCalculator.daysElapsedInMonth(today);
        BigDecimal averageDailySpend = AnalyticsCalculator.averageDailySpend(totalSpent, daysElapsed);

        var budget = budgetService.getCurrentMonthBudget(user).orElse(null);
        if (budget == null) {
            return new SpendingSummary(
                    monthLabel(currentMonth),
                    totalSpent,
                    null,
                    null,
                    daysLeft,
                    BigDecimal.ZERO,
                    daysElapsed,
                    averageDailySpend,
                    BigDecimal.ZERO
            );
        }

        BigDecimal remaining = AnalyticsCalculator.remaining(budget.getAmount(), totalSpent);
        BigDecimal safeDailySpend = AnalyticsCalculator.safeDailySpend(remaining, daysLeft);
        BigDecimal budgetUsedPercentage = AnalyticsCalculator.percentage(totalSpent, budget.getAmount());

        return new SpendingSummary(
                monthLabel(currentMonth),
                totalSpent,
                budget.getAmount(),
                remaining,
                daysLeft,
                safeDailySpend,
                daysElapsed,
                averageDailySpend,
                budgetUsedPercentage
        );
    }

    @Transactional(readOnly = true)
    public List<CategoryBreakdownItem> categoryBreakdown(WalletUser user) {
        YearMonth currentMonth = YearMonth.now(clock);
        MonthRange range = MonthRange.from(currentMonth);
        BigDecimal totalSpent = expenseRepository.sumAmountForUserBetween(user, range.startInclusive(), range.endExclusive());
        return expenseRepository.sumByCategoryForUserBetween(user, range.startInclusive(), range.endExclusive())
                .stream()
                .map(item -> new CategoryBreakdownItem(
                        item.getCategory(),
                        MoneyUtils.money(item.getTotal()),
                        AnalyticsCalculator.percentage(item.getTotal(), totalSpent)
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryBreakdownItem categorySummary(WalletUser user, ExpenseCategory category) {
        YearMonth currentMonth = YearMonth.now(clock);
        MonthRange range = MonthRange.from(currentMonth);
        BigDecimal totalSpent = expenseRepository.sumAmountForUserBetween(user, range.startInclusive(), range.endExclusive());
        BigDecimal categoryTotal = expenseRepository.sumAmountForUserAndCategoryBetween(
                user,
                category,
                range.startInclusive(),
                range.endExclusive()
        );
        return new CategoryBreakdownItem(
                category,
                MoneyUtils.money(categoryTotal),
                AnalyticsCalculator.percentage(categoryTotal, totalSpent)
        );
    }

    private String monthLabel(YearMonth month) {
        return month.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }
}
