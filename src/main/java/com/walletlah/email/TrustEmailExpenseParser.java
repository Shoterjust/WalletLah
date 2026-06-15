package com.walletlah.email;

import java.time.Clock;
import java.util.regex.Pattern;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(40)
class TrustEmailExpenseParser extends AbstractBankEmailExpenseParser {

    private static final Pattern[] MERCHANT_PATTERNS = {
            Pattern.compile("(?im)(?:at|merchant|paid to)\\s*[:\\-]?\\s*([^\\n\\r.]{2,80})")
    };

    TrustEmailExpenseParser(Clock clock) {
        super(clock);
    }

    @Override
    protected String provider() {
        return "TRUST";
    }

    @Override
    protected String[] signals() {
        return new String[]{"trust bank", "trust card"};
    }

    @Override
    protected Pattern[] merchantPatterns() {
        return MERCHANT_PATTERNS;
    }
}
