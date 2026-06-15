package com.walletlah.email;

import java.time.Clock;
import java.util.regex.Pattern;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(30)
class UobEmailExpenseParser extends AbstractBankEmailExpenseParser {

    private static final Pattern[] MERCHANT_PATTERNS = {
            Pattern.compile("(?im)(?:at|merchant)\\s+([^\\n\\r.]{2,80})"),
            Pattern.compile("(?im)spent\\s+(?:on\\s+)?(?:your\\s+)?(?:uob\\s+)?card\\s+at\\s+([^\\n\\r.]{2,80})")
    };

    UobEmailExpenseParser(Clock clock) {
        super(clock);
    }

    @Override
    protected String provider() {
        return "UOB";
    }

    @Override
    protected String[] signals() {
        return new String[]{"uob", "unialert"};
    }

    @Override
    protected Pattern[] merchantPatterns() {
        return MERCHANT_PATTERNS;
    }
}
