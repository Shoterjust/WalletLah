package com.walletlah.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "walletlah")
public record AppProperties(String zoneId) {
}
