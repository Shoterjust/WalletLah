package com.walletlah.bot;

public record TelegramUserContext(
        Long telegramUserId,
        Long telegramChatId,
        String username,
        String firstName
) {
}
