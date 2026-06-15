package com.walletlah.email;

import java.time.Clock;
import java.util.regex.Pattern;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
class DbsEmailExpenseParser extends AbstractBankEmailExpenseParser {

    private static final Pattern[] MERCHANT_PATTERNS = {
            Pattern.compile("(?im)(?:at|merchant)\\s+([^\\n\\r.]{2,80})"),
            Pattern.compile("(?im)(?:transaction|purchase)\\s+(?:made\\s+)?at\\s+([^\\n\\r.]{2,80})")
    };

    DbsEmailExpenseParser(Clock clock) {
        super(clock);
    }

    @Override
    protected String provider() {
        return "DBS";
    }

    @Override
    protected String[] signals() {
        return new String[]{"dbs", "posb"};
    }

    @Override
    protected Pattern[] merchantPatterns() {
        return MERCHANT_PATTERNS;
    }
}
