package com.walletlah.recurring;

import com.walletlah.common.UserFacingException;
import com.walletlah.expense.ExpenseCategory;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AddRecurringExpenseParser {

    private final Clock clock;

    public AddRecurringExpenseParser(Clock clock) {
        this.clock = clock;
    }

    public AddRecurringExpenseRequest parse(String body) {
        if (!StringUtils.hasText(body)) {
            throw invalidFormat();
        }

        List<String> tokens = Arrays.stream(body.trim().split("\\s+"))
                .filter(StringUtils::hasText)
                .toList();
        if (tokens.size() < 3) {
            throw invalidFormat();
        }

        BigDecimal amount = parseAmount(tokens.get(0));
        ExpenseCategory category = ExpenseCategory.from(tokens.get(1))
                .orElseThrow(() -> new UserFacingException("Unknown category. Use /categories to see valid categories."));

        LocalDate nextRunDate = LocalDate.now(clock);
        int frequencyIndex = tokens.size() - 1;
        LocalDate possibleDate = parseDateOrNull(tokens.get(tokens.size() - 1));
        if (possibleDate != null) {
            nextRunDate = possibleDate;
            frequencyIndex = tokens.size() - 2;
        }

        if (frequencyIndex < 2) {
            throw invalidFormat();
        }

        RecurringFrequency frequency = RecurringFrequency.parse(tokens.get(frequencyIndex));
        String description = String.join(" ", tokens.subList(2, frequencyIndex)).trim();
        if (!StringUtils.hasText(description)) {
            description = category.displayName();
        }
        description = limit(description, 255);

        return new AddRecurringExpenseRequest(amount, category, description, description, frequency, nextRunDate);
    }

    private BigDecimal parseAmount(String value) {
        try {
            BigDecimal amount = new BigDecimal(value.replace("S$", "").replace("$", "").trim());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new UserFacingException("Recurring amount must be more than zero.");
            }
            return amount;
        } catch (NumberFormatException e) {
            throw new UserFacingException("I could not read that amount. Try: /recurring_add 14.99 subscriptions Spotify monthly");
        }
    }

    private LocalDate parseDateOrNull(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private UserFacingException invalidFormat() {
        return new UserFacingException("Use /recurring_add 14.99 subscriptions Spotify monthly or /recurring_add 80 transport concession monthly 2026-07-01");
    }

    private String limit(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
