package com.walletlah.email;

import com.walletlah.common.UserFacingException;
import com.walletlah.expense.ExpenseCategory;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;

abstract class AbstractBankEmailExpenseParser implements BankEmailParser, Ordered {

    private final Clock clock;

    AbstractBankEmailExpenseParser(Clock clock) {
        this.clock = clock;
    }

    @Override
    public boolean supports(EmailExpenseIngestRequest request) {
        String text = EmailParsingSupport.combine(request.subject(), request.body()).toLowerCase(Locale.ROOT);
        String sender = request.sender() == null ? "" : request.sender().toLowerCase(Locale.ROOT);
        for (String signal : signals()) {
            String normalizedSignal = signal.toLowerCase(Locale.ROOT);
            if (text.contains(normalizedSignal) || sender.contains(normalizedSignal)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ParsedEmailExpense parse(EmailExpenseIngestRequest request) {
        String text = EmailParsingSupport.combine(request.subject(), request.body());
        EmailParsingSupport.rejectNonExpense(text);

        BigDecimal amount = EmailParsingSupport.findAmount(text)
                .orElseThrow(() -> new UserFacingException("I could not find a transaction amount in that " + provider() + " email."));
        String merchant = findBankMerchant(text)
                .or(() -> EmailParsingSupport.findMerchant(text))
                .orElse(provider() + " transaction");
        ExpenseCategory category = EmailParsingSupport.inferCategory(text + " " + merchant);
        LocalDate expenseDate = EmailParsingSupport.findDateOrDefault(request, clock);

        return new ParsedEmailExpense(amount, category, merchant, expenseDate, provider(), text);
    }

    @Override
    public int getOrder() {
        return 100;
    }

    protected abstract String provider();

    protected abstract String[] signals();

    protected abstract Pattern[] merchantPatterns();

    private Optional<String> findBankMerchant(String text) {
        for (Pattern pattern : merchantPatterns()) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String merchant = EmailParsingSupport.cleanMerchant(matcher.group(1));
                if (StringUtils.hasText(merchant)) {
                    return Optional.of(merchant);
                }
            }
        }
        return Optional.empty();
    }
}
