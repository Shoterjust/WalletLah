package com.walletlah.budget;

import com.walletlah.user.WalletUser;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonthlyBudgetRepository extends JpaRepository<MonthlyBudget, Long> {

    Optional<MonthlyBudget> findByUserAndMonth(WalletUser user, LocalDate month);
}
