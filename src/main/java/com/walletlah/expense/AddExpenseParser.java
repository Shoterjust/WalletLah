package com.walletlah.expense;

import com.walletlah.common.UserFacingException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AddExpenseParser {

    private final Clock clock;

    public AddExpenseParser(Clock clock) {
        this.clock = clock;
    }

    public AddExpenseRequest parse(String input) {
        if (!StringUtils.hasText(input)) {
            throw invalidExpenseMessage();
        }

        List<String> tokens = tokenize(input);
        if (tokens.size() < 2) {
            throw invalidExpenseMessage();
        }

        Parsed parsed = parseAmountFirst(tokens);
        if (parsed == null) {
            parsed = parseCategoryFirst(tokens);
        }
        if (parsed == null) {
            throw invalidExpenseMessage();
        }

        if (parsed.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new UserFacingException("Expense amount must be more than zero.");
        }

        String description = StringUtils.hasText(parsed.description())
                ? parsed.description()
                : parsed.category().displayName();

        return new AddExpenseRequest(parsed.amount(), parsed.category(), description, LocalDate.now(clock));
    }

    private Parsed parseAmountFirst(List<String> tokens) {
        BigDecimal amount = parseAmountOrNull(tokens.get(0));
        if (amount == null) {
            return null;
        }

        ExpenseCategory category = ExpenseCategory.from(tokens.get(1)).orElse(ExpenseCategory.OTHERS);
        int descriptionStart = category == ExpenseCategory.OTHERS
                && ExpenseCategory.from(tokens.get(1)).isEmpty()
                ? 1
                : 2;
        return new Parsed(amount, category, join(tokens, descriptionStart));
    }

    private Parsed parseCategoryFirst(List<String> tokens) {
        ExpenseCategory category = ExpenseCategory.from(tokens.get(0)).orElse(null);
        if (category == null) {
            return null;
        }
        BigDecimal amount = parseAmountOrNull(tokens.get(1));
        if (amount == null) {
            return null;
        }
        return new Parsed(amount, category, join(tokens, 2));
    }

    private BigDecimal parseAmountOrNull(String token) {
        String cleaned = token.trim()
                .toLowerCase()
                .replace("sgd", "")
                .replace("s$", "")
                .replace("$", "");
        if (!StringUtils.hasText(cleaned)) {
            return null;
        }
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<String> tokenize(String input) {
        String[] rawTokens = input.trim().split("\\s+");
        List<String> tokens = new ArrayList<>();
        for (String rawToken : rawTokens) {
            if (StringUtils.hasText(rawToken)) {
                tokens.add(rawToken.trim());
            }
        }
        return tokens;
    }

    private String join(List<String> tokens, int startInclusive) {
        if (startInclusive >= tokens.size()) {
            return "";
        }
        return String.join(" ", tokens.subList(startInclusive, tokens.size()));
    }

    private UserFacingException invalidExpenseMessage() {
        return new UserFacingException("I could not read that expense.\n\nUse:\n/add 5.50 food chicken rice\n\nSee categories with /categories");
    }

    private record Parsed(BigDecimal amount, ExpenseCategory category, String description) {
    }
}
