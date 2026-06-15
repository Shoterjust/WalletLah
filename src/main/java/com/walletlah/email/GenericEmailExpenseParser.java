package com.walletlah.email;

import com.walletlah.common.UserFacingException;
import com.walletlah.expense.ExpenseCategory;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

@Component
class GenericEmailExpenseParser implements BankEmailParser, Ordered {

    private final Clock clock;

    GenericEmailExpenseParser(Clock clock) {
        this.clock = clock;
    }

    @Override
    public boolean supports(EmailExpenseIngestRequest request) {
        return true;
    }

    @Override
    public ParsedEmailExpense parse(EmailExpenseIngestRequest request) {
        String text = EmailParsingSupport.combine(request.subject(), request.body());
        EmailParsingSupport.rejectNonExpense(text);

        BigDecimal amount = EmailParsingSupport.findAmount(text)
                .orElseThrow(() -> new UserFacingException("I could not find a transaction amount in that email."));
        String merchant = EmailParsingSupport.findMerchant(text)
                .orElse("Email transaction");
        ExpenseCategory category = EmailParsingSupport.inferCategory(text + " " + merchant);
        LocalDate expenseDate = EmailParsingSupport.findDateOrDefault(request, clock);

        return new ParsedEmailExpense(amount, category, merchant, expenseDate, "GENERIC", text);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
