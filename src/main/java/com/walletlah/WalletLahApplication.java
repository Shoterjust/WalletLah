package com.walletlah;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class WalletLahApplication {

    public static void main(String[] args) {
        SpringApplication.run(WalletLahApplication.class, args);
    }
}
