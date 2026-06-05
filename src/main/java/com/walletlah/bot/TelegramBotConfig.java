package com.walletlah.bot;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TelegramBotConfig {

    @Bean
    @ConditionalOnProperty(name = "walletlah.bot.enabled", havingValue = "true")
    WalletLahBot walletLahBot(TelegramBotService telegramBotService, TelegramUpdateHandler updateHandler) {
        return new WalletLahBot(telegramBotService, updateHandler);
    }
}
