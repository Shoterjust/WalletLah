package com.walletlah.dashboard.auth;

import com.walletlah.user.WalletUser;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DashboardLinkCodeRepository extends JpaRepository<DashboardLinkCode, Long> {

    List<DashboardLinkCode> findByUserAndConsumedAtIsNull(WalletUser user);

    List<DashboardLinkCode> findByCodeHashAndConsumedAtIsNullOrderByCreatedAtDesc(String codeHash);
}
