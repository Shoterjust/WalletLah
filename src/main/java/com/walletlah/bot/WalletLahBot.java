package com.walletlah.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

public class WalletLahBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private static final Logger log = LoggerFactory.getLogger(WalletLahBot.class);

    private final String botToken;
    private final TelegramClient telegramClient;
    private final BotCommandRouter commandRouter;

    public WalletLahBot(BotProperties properties, BotCommandRouter commandRouter) {
        if (!StringUtils.hasText(properties.token())) {
            throw new IllegalStateException("TELEGRAM_BOT_TOKEN is required when TELEGRAM_BOT_ENABLED=true");
        }
        this.botToken = properties.token();
        this.telegramClient = new OkHttpTelegramClient(properties.token());
        this.commandRouter = commandRouter;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        var message = update.getMessage();
        Long chatId = message.getChatId();
        User from = message.getFrom();
        if (from == null) {
            send(chatId, "I could not identify the Telegram user for this message.");
            return;
        }

        TelegramUserContext context = new TelegramUserContext(
                from.getId(),
                chatId,
                from.getUserName(),
                from.getFirstName()
        );

        String response = commandRouter.handle(context, message.getText());
        send(chatId, response);
    }

    private void send(Long chatId, String text) {
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
}
