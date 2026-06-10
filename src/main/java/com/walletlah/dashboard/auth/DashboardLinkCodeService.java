package com.walletlah.dashboard.auth;

import com.walletlah.common.UserFacingException;
import com.walletlah.dashboard.DashboardProperties;
import com.walletlah.user.WalletUser;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DashboardLinkCodeService {

    private static final int MAX_CODE_GENERATION_ATTEMPTS = 10;

    private final DashboardLinkCodeRepository linkCodeRepository;
    private final DashboardProperties properties;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public DashboardLinkCodeService(
            DashboardLinkCodeRepository linkCodeRepository,
            DashboardProperties properties,
            Clock clock
    ) {
        this.linkCodeRepository = linkCodeRepository;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public String issueCode(WalletUser user) {
        Instant now = Instant.now(clock);
        consumeExistingCodes(user, now);

        for (int attempt = 0; attempt < MAX_CODE_GENERATION_ATTEMPTS; attempt++) {
            String code = generateCode();
            String codeHash = hash(code);
            boolean codeInUse = linkCodeRepository.findByCodeHashAndConsumedAtIsNullOrderByCreatedAtDesc(codeHash)
                    .stream()
                    .anyMatch(existing -> existing.isUsable(now));
            if (!codeInUse) {
                linkCodeRepository.save(new DashboardLinkCode(
                        user,
                        codeHash,
                        now.plus(properties.linkCodeTtlMinutes(), ChronoUnit.MINUTES)
                ));
                return code;
            }
        }

        throw new UserFacingException("I could not create a dashboard login code. Please try again.");
    }

    @Transactional
    public WalletUser consumeCode(String rawCode) {
        String code = normalizeCode(rawCode);
        Instant now = Instant.now(clock);
        DashboardLinkCode linkCode = linkCodeRepository.findByCodeHashAndConsumedAtIsNullOrderByCreatedAtDesc(hash(code))
                .stream()
                .filter(existing -> existing.isUsable(now))
                .findFirst()
                .orElseThrow(() -> new UserFacingException("Invalid or expired dashboard code."));

        linkCode.consume(now);
        return linkCode.getUser();
    }

    private void consumeExistingCodes(WalletUser user, Instant now) {
        linkCodeRepository.findByUserAndConsumedAtIsNull(user)
                .stream()
                .filter(existing -> existing.isUsable(now))
                .forEach(existing -> existing.consume(now));
    }

    private String generateCode() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    private String normalizeCode(String rawCode) {
        if (!StringUtils.hasText(rawCode)) {
            throw new UserFacingException("Enter the 6-digit code from Telegram.");
        }
        String code = rawCode.trim();
        if (!code.matches("^\\d{6}$")) {
            throw new UserFacingException("Dashboard code must be 6 digits.");
        }
        return code;
    }

    private String hash(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(code.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required for dashboard link codes", e);
        }
    }
}
