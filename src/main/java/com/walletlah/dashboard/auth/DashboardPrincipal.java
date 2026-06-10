package com.walletlah.dashboard.auth;

import com.walletlah.user.WalletUser;

public record DashboardPrincipal(
        Long userId,
        Long telegramUserId,
        String displayName
) {

    public static DashboardPrincipal from(WalletUser user) {
        String displayName = user.getFirstName() != null && !user.getFirstName().isBlank()
                ? user.getFirstName()
                : user.getUsername();
        return new DashboardPrincipal(user.getId(), user.getTelegramUserId(), displayName);
    }
}
