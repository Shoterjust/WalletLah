package com.walletlah.dashboard.auth;

import com.walletlah.user.WalletUser;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DashboardLinkCodeRepository extends JpaRepository<DashboardLinkCode, Long> {

    List<DashboardLinkCode> findByUserAndConsumedAtIsNull(WalletUser user);

    List<DashboardLinkCode> findByCodeHashAndConsumedAtIsNullOrderByCreatedAtDesc(String codeHash);

    @Query("""
            select code
            from DashboardLinkCode code
            join fetch code.user
            where code.codeHash = :codeHash
              and code.consumedAt is null
            order by code.createdAt desc
            """)
    List<DashboardLinkCode> findUsableByCodeHashWithUser(@Param("codeHash") String codeHash);
}
