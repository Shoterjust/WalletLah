package com.walletlah.receipt;

import com.walletlah.common.UserFacingException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class ReceiptScanRateLimiter {

    private final Map<Long, Instant> lastScanByUser = new ConcurrentHashMap<>();
    private final ReceiptScanProperties properties;
    private final Clock clock;

    public ReceiptScanRateLimiter(ReceiptScanProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public void checkAllowed(Long telegramUserId) {
        Instant now = Instant.now(clock);
        Instant lastScan = lastScanByUser.get(telegramUserId);
        if (lastScan != null) {
            long secondsSinceLastScan = Duration.between(lastScan, now).toSeconds();
            if (secondsSinceLastScan < properties.rateLimitSeconds()) {
                long waitSeconds = properties.rateLimitSeconds() - secondsSinceLastScan;
                throw new UserFacingException("Please wait " + waitSeconds + " seconds before scanning another receipt.");
            }
        }
        lastScanByUser.put(telegramUserId, now);
    }
}
