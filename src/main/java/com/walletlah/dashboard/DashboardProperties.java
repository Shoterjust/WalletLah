package com.walletlah.dashboard;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "walletlah.dashboard")
public record DashboardProperties(
        List<String> allowedOrigins,
        long linkCodeTtlMinutes
) {
}
