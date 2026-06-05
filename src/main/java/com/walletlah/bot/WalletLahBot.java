package com.walletlah.bot;

import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;

public class WalletLahBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private final TelegramBotService telegramBotService;
    private final TelegramUpdateHandler updateHandler;

    public WalletLahBot(TelegramBotService telegramBotService, TelegramUpdateHandler updateHandler) {
        this.telegramBotService = telegramBotService;
        this.updateHandler = updateHandler;
    }

    @Override
    public String getBotToken() {
        return telegramBotService.botToken();
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        if (!update.hasMessage()) {
            return;
        }
        Long chatId = update.getMessage().getChatId();
        updateHandler.handle(update).ifPresent(response -> telegramBotService.send(chatId, response));
    }
}
