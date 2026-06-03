package com.walletlah.common;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfig {

    @Bean
    Clock walletLahClock(AppProperties properties) {
        return Clock.system(ZoneId.of(properties.zoneId()));
    }
}
