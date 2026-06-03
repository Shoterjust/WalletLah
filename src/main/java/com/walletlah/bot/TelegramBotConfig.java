package com.walletlah.bot;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TelegramBotConfig {

    @Bean
    @ConditionalOnProperty(name = "walletlah.bot.enabled", havingValue = "true")
    WalletLahBot walletLahBot(BotProperties properties, BotCommandRouter commandRouter) {
        return new WalletLahBot(properties, commandRouter);
    }
}
