package com.walletlah.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletUserRepository extends JpaRepository<WalletUser, Long> {

    Optional<WalletUser> findByTelegramUserId(Long telegramUserId);

    Optional<WalletUser> findByEmailAddressIgnoreCase(String emailAddress);
}
