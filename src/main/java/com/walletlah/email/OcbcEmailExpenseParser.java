package com.walletlah.email;

import java.time.Clock;
import java.util.regex.Pattern;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
class OcbcEmailExpenseParser extends AbstractBankEmailExpenseParser {

    private static final Pattern[] MERCHANT_PATTERNS = {
            Pattern.compile("(?im)(?:at|merchant|description)\\s*[:\\-]?\\s*([^\\n\\r.]{2,80})"),
            Pattern.compile("(?im)(?:transaction|card transaction)\\s+(?:was\\s+)?(?:made\\s+)?at\\s+([^\\n\\r.]{2,80})")
    };

    OcbcEmailExpenseParser(Clock clock) {
        super(clock);
    }

    @Override
    protected String provider() {
        return "OCBC";
    }

    @Override
    protected String[] signals() {
        return new String[]{"ocbc"};
    }

    @Override
    protected Pattern[] merchantPatterns() {
        return MERCHANT_PATTERNS;
    }
}
