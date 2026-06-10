package com.walletlah.dashboard.auth;

public record DashboardAuthResponse(
        Long userId,
        Long telegramUserId,
        String displayName
) {

    public static DashboardAuthResponse from(DashboardPrincipal principal) {
        return new DashboardAuthResponse(principal.userId(), principal.telegramUserId(), principal.displayName());
    }
}
