package com.walletlah.receipt;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PendingExpenseRepository extends JpaRepository<PendingExpense, Long> {

    Optional<PendingExpense> findFirstByTelegramUserIdAndStatusOrderByCreatedAtDesc(
            Long telegramUserId,
            PendingExpenseStatus status
    );
}
