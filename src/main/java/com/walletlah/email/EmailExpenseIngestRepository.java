package com.walletlah.email;

import com.walletlah.user.WalletUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailExpenseIngestRepository extends JpaRepository<EmailExpenseIngest, Long> {

    boolean existsByUserAndSourceMessageId(WalletUser user, String sourceMessageId);
}
