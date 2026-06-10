package com.walletlah.recurring;

import com.walletlah.user.WalletUser;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurringExpenseRepository extends JpaRepository<RecurringExpense, Long> {

    List<RecurringExpense> findByUserAndActiveTrueOrderByNextRunDateAscIdAsc(WalletUser user);

    Optional<RecurringExpense> findByIdAndUserAndActiveTrue(Long id, WalletUser user);

    List<RecurringExpense> findByActiveTrueAndNextRunDateLessThanEqualOrderByNextRunDateAscIdAsc(LocalDate date);
}
