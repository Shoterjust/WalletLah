package com.walletlah.user;

import com.walletlah.bot.TelegramUserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final WalletUserRepository userRepository;

    public UserService(WalletUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public WalletUser registerOrUpdate(TelegramUserContext context) {
        return userRepository.findByTelegramUserId(context.telegramUserId())
                .map(user -> {
                    user.updateTelegramProfile(context.telegramChatId(), context.username(), context.firstName());
                    return user;
                })
                .orElseGet(() -> userRepository.save(new WalletUser(
                        context.telegramUserId(),
                        context.telegramChatId(),
                        context.username(),
                        context.firstName()
                )));
    }
}
