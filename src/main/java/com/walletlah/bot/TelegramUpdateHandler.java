package com.walletlah.bot;

import com.walletlah.common.UserFacingException;
import com.walletlah.receipt.ReceiptPhotoHandler;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

@Component
@ConditionalOnProperty(name = "walletlah.bot.enabled", havingValue = "true")
public class TelegramUpdateHandler {

    private static final Logger log = LoggerFactory.getLogger(TelegramUpdateHandler.class);

    private final BotCommandRouter commandRouter;
    private final ReceiptPhotoHandler receiptPhotoHandler;

    public TelegramUpdateHandler(BotCommandRouter commandRouter, ReceiptPhotoHandler receiptPhotoHandler) {
        this.commandRouter = commandRouter;
        this.receiptPhotoHandler = receiptPhotoHandler;
    }

    public Optional<String> handle(Update update) {
        if (!update.hasMessage()) {
            return Optional.empty();
        }

        var message = update.getMessage();
        User from = message.getFrom();
        if (from == null) {
            return Optional.of("I could not identify the Telegram user for this message.");
        }

        TelegramUserContext context = new TelegramUserContext(
                from.getId(),
                message.getChatId(),
                from.getUserName(),
                from.getFirstName()
        );

        try {
            if (message.hasPhoto()) {
                return Optional.of(receiptPhotoHandler.handle(context, message.getPhoto()));
            }
            if (message.hasText()) {
                return Optional.of(commandRouter.handle(context, message.getText()));
            }
            if (message.hasDocument()) {
                return Optional.of("Please send receipts as Telegram photos for now. You can also log manually with /add 5.50 food chicken rice");
            }
            return Optional.empty();
        } catch (UserFacingException e) {
            return Optional.of(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Unexpected Telegram update failure", e);
            return Optional.of("Something went wrong while handling that. Please try again.");
        }
    }
}
