package com.walletlah.receipt;

public interface ReceiptOcrService {

    ReceiptScanResult analyze(byte[] imageBytes);
}
