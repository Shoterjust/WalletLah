package com.walletlah.budget;

import com.walletlah.common.MoneyUtils;
import com.walletlah.user.WalletUser;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.YearMonth;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BudgetService {

    private final MonthlyBudgetRepository budgetRepository;
    private final Clock clock;

    public BudgetService(MonthlyBudgetRepository budgetRepository, Clock clock) {
        this.budgetRepository = budgetRepository;
        this.clock = clock;
    }

    @Transactional
    public MonthlyBudget setCurrentMonthBudget(WalletUser user, BigDecimal amount) {
        BigDecimal money = MoneyUtils.money(amount);
        var month = YearMonth.now(clock).atDay(1);
        return budgetRepository.findByUserAndMonth(user, month)
                .map(existing -> {
                    existing.updateAmount(money);
                    return existing;
                })
                .orElseGet(() -> budgetRepository.save(new MonthlyBudget(user, month, money)));
    }

    @Transactional(readOnly = true)
    public Optional<MonthlyBudget> getCurrentMonthBudget(WalletUser user) {
        return budgetRepository.findByUserAndMonth(user, YearMonth.now(clock).atDay(1));
    }
}
