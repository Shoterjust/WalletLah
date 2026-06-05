package com.walletlah.receipt;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "walletlah.receipts")
public record ReceiptScanProperties(
        boolean enabled,
        String awsRegion,
        long maxFileSizeBytes,
        BigDecimal minConfidence,
        int pendingExpiryMinutes,
        int rateLimitSeconds
) {
}
