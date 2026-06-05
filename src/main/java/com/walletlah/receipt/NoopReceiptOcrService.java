package com.walletlah.receipt;

import com.walletlah.common.UserFacingException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "walletlah.receipts.enabled", havingValue = "false", matchIfMissing = true)
public class NoopReceiptOcrService implements ReceiptOcrService {

    @Override
    public ReceiptScanResult analyze(byte[] imageBytes) {
        throw new UserFacingException("Receipt scanning is not enabled yet. You can still log manually with /add 5.50 food chicken rice");
    }
}
