package com.walletlah.user;

import com.walletlah.bot.TelegramUserContext;
import com.walletlah.common.UserFacingException;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

    @Transactional
    public WalletUser linkEmail(TelegramUserContext context, String rawEmailAddress) {
        String emailAddress = normalizeEmail(rawEmailAddress);
        walletUserWithEmail(emailAddress)
                .filter(existingUser -> !existingUser.getTelegramUserId().equals(context.telegramUserId()))
                .ifPresent(existingUser -> {
                    throw new UserFacingException("That email is already linked to another WalletLah user.");
                });

        WalletUser user = registerOrUpdate(context);
        user.updateEmailAddress(emailAddress);
        return user;
    }

    @Transactional(readOnly = true)
    public Optional<WalletUser> walletUserWithEmail(String rawEmailAddress) {
        if (!StringUtils.hasText(rawEmailAddress)) {
            return Optional.empty();
        }
        return userRepository.findByEmailAddressIgnoreCase(normalizeEmail(rawEmailAddress));
    }

    private String normalizeEmail(String rawEmailAddress) {
        if (!StringUtils.hasText(rawEmailAddress)) {
            throw new UserFacingException("Use /email your@email.com to link receipt auto-logging.");
        }

        String emailAddress = rawEmailAddress.trim().toLowerCase(Locale.ROOT);
        if (!emailAddress.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new UserFacingException("I could not read that email. Try /email your@email.com");
        }
        return emailAddress;
    }
}
