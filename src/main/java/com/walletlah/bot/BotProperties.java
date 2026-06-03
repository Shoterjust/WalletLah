package com.walletlah.bot;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "walletlah.bot")
public record BotProperties(boolean enabled, String token) {
}
