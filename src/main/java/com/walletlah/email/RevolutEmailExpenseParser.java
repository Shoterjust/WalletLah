package com.walletlah.email;

import java.time.Clock;
import java.util.regex.Pattern;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(60)
class RevolutEmailExpenseParser extends AbstractBankEmailExpenseParser {

    private static final Pattern[] MERCHANT_PATTERNS = {
            Pattern.compile("(?im)(?:at|merchant|paid to)\\s*[:\\-]?\\s*([^\\n\\r.]{2,80})")
    };

    RevolutEmailExpenseParser(Clock clock) {
        super(clock);
    }

    @Override
    protected String provider() {
        return "REVOLUT";
    }

    @Override
    protected String[] signals() {
        return new String[]{"revolut"};
    }

    @Override
    protected Pattern[] merchantPatterns() {
        return MERCHANT_PATTERNS;
    }
}
