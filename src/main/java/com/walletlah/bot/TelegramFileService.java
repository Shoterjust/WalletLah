package com.walletlah.bot;

import com.walletlah.common.UserFacingException;
import com.walletlah.receipt.ReceiptScanProperties;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Service
@ConditionalOnProperty(name = "walletlah.bot.enabled", havingValue = "true")
public class TelegramFileService {

    private final TelegramBotService telegramBotService;
    private final ReceiptScanProperties properties;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public TelegramFileService(TelegramBotService telegramBotService, ReceiptScanProperties properties) {
        this.telegramBotService = telegramBotService;
        this.properties = properties;
    }

    public byte[] downloadFile(String fileId, Integer declaredFileSize) {
        if (declaredFileSize != null && declaredFileSize > properties.maxFileSizeBytes()) {
            throw new UserFacingException("That receipt photo is too large. Please send a smaller image under 5 MB.");
        }

        try {
            var file = telegramBotService.getFile(fileId);
            if (file.getFileSize() != null && file.getFileSize() > properties.maxFileSizeBytes()) {
                throw new UserFacingException("That receipt photo is too large. Please send a smaller image under 5 MB.");
            }

            URI downloadUri = URI.create("https://api.telegram.org/file/bot"
                    + telegramBotService.botToken()
                    + "/"
                    + file.getFilePath());
            HttpRequest request = HttpRequest.newBuilder(downloadUri)
                    .timeout(Duration.ofSeconds(20))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new UserFacingException("I could not download that receipt photo from Telegram. Please try sending it again.");
            }
            byte[] bytes = response.body();
            if (bytes.length > properties.maxFileSizeBytes()) {
                throw new UserFacingException("That receipt photo is too large. Please send a smaller image under 5 MB.");
            }
            return bytes;
        } catch (TelegramApiException | IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new UserFacingException("I could not download that receipt photo from Telegram. Please try sending it again.");
        }
    }
}
