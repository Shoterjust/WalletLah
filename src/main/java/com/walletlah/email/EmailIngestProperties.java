package com.walletlah.email;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "walletlah.email-ingest")
public record EmailIngestProperties(boolean enabled, String token) {

    boolean matchesToken(String candidate) {
        if (!StringUtils.hasText(token) || !StringUtils.hasText(candidate)) {
            return false;
        }
        return MessageDigest.isEqual(
                token.getBytes(StandardCharsets.UTF_8),
                candidate.getBytes(StandardCharsets.UTF_8)
        );
    }
}
