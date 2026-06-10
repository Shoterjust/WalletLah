package com.walletlah.receipt;

import com.walletlah.bot.TelegramFileService;
import com.walletlah.bot.TelegramResponseFormatter;
import com.walletlah.bot.TelegramUserContext;
import com.walletlah.common.UserFacingException;
import com.walletlah.user.UserService;
import java.util.Comparator;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.photo.PhotoSize;

@Component
@ConditionalOnProperty(name = "walletlah.bot.enabled", havingValue = "true")
public class ReceiptPhotoHandler {

    private final UserService userService;
    private final TelegramFileService telegramFileService;
    private final ReceiptOcrService receiptOcrService;
    private final PendingExpenseService pendingExpenseService;
    private final ReceiptScanRateLimiter rateLimiter;
    private final TelegramResponseFormatter formatter;
    private final ReceiptScanProperties properties;

    public ReceiptPhotoHandler(
            UserService userService,
            TelegramFileService telegramFileService,
            ReceiptOcrService receiptOcrService,
            PendingExpenseService pendingExpenseService,
            ReceiptScanRateLimiter rateLimiter,
            TelegramResponseFormatter formatter,
            ReceiptScanProperties properties
    ) {
        this.userService = userService;
        this.telegramFileService = telegramFileService;
        this.receiptOcrService = receiptOcrService;
        this.pendingExpenseService = pendingExpenseService;
        this.rateLimiter = rateLimiter;
        this.formatter = formatter;
        this.properties = properties;
    }

    public String handle(TelegramUserContext context, List<PhotoSize> photos) {
        userService.registerOrUpdate(context);
        if (!properties.enabled()) {
            throw new UserFacingException("Receipt scanning is not enabled yet. You can still log manually with /add 5.50 food chicken rice");
        }
        PhotoSize photo = largestPhoto(photos);
        rateLimiter.checkAllowed(context.telegramUserId());

        byte[] imageBytes = telegramFileService.downloadFile(photo.getFileId(), photo.getFileSize());
        ReceiptScanResult scanResult = receiptOcrService.analyze(imageBytes);
        PendingExpense pendingExpense = pendingExpenseService.create(
                context.telegramUserId(),
                scanResult,
                photo.getFileId()
        );
        return formatter.receiptScanned(pendingExpense);
    }

    private PhotoSize largestPhoto(List<PhotoSize> photos) {
        if (photos == null || photos.isEmpty()) {
            throw new UserFacingException("Please send the receipt as a photo image.");
        }
        return photos.stream()
                .max(Comparator.comparingInt(photo -> photo.getWidth() * photo.getHeight()))
                .orElseThrow(() -> new UserFacingException("Please send the receipt as a photo image."));
    }
}
