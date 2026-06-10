package com.walletlah.dashboard.auth;

import jakarta.validation.constraints.NotBlank;

public record DashboardLinkCodeLoginRequest(
        @NotBlank String code
) {
}
