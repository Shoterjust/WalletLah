package com.walletlah.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Service
@ConditionalOnProperty(name = "walletlah.bot.enabled", havingValue = "true")
public class TelegramBotService {

    private static final Logger log = LoggerFactory.getLogger(TelegramBotService.class);

    private final String botToken;
    private final TelegramClient telegramClient;

    public TelegramBotService(BotProperties properties) {
        if (!StringUtils.hasText(properties.token())) {
            throw new IllegalStateException("TELEGRAM_BOT_TOKEN is required when TELEGRAM_BOT_ENABLED=true");
        }
        this.botToken = properties.token();
        this.telegramClient = new OkHttpTelegramClient(properties.token());
    }

    public String botToken() {
        return botToken;
    }

    public void send(Long chatId, String text) {
        try {
            SendMessage message = SendMessage.builder()
                    .chatId(chatId)
                    .text(text)
                    .build();
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send Telegram message to chat {}", chatId, e);
        }
    }

    public File getFile(String fileId) throws TelegramApiException {
        return telegramClient.execute(GetFile.builder()
                .fileId(fileId)
                .build());
    }
}
