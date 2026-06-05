package com.walletlah.email;

import com.walletlah.common.UserFacingException;
import com.walletlah.expense.ExpenseCategory;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class EmailExpenseParser {

    private static final Pattern CURRENCY_AMOUNT = Pattern.compile(
            "(?i)(?:SGD|S\\$|\\$)\\s*([0-9][0-9,]*(?:\\.\\d{2})?)|([0-9][0-9,]*(?:\\.\\d{2})?)\\s*(?:SGD)"
    );
    private static final Pattern KEYWORD_AMOUNT = Pattern.compile(
            "(?i)(?:amount|transaction amount|spent|charged|paid|purchase)\\D{0,24}([0-9][0-9,]*\\.\\d{2})"
    );
    private static final List<String> NON_EXPENSE_PHRASES = List.of(
            "refund",
            "refunded",
            "reversal",
            "reversed",
            "credited",
            "cashback",
            "rebate",
            "payment received",
            "credit card payment"
    );
    private static final List<Pattern> MERCHANT_PATTERNS = List.of(
            Pattern.compile("(?im)(?:merchant|description|transaction at|purchase at|spent at|charged at|paid to)\\s*[:\\-]?\\s*([^\\n\\r.]{2,80})"),
            Pattern.compile("(?im)\\bat\\s+([^\\n\\r.]{2,80})")
    );

    private final Clock clock;

    public EmailExpenseParser(Clock clock) {
        this.clock = clock;
    }

    public ParsedEmailExpense parse(EmailExpenseIngestRequest request) {
        String text = combine(request.subject(), request.body());
        String lowerText = text.toLowerCase(Locale.ROOT);

        if (NON_EXPENSE_PHRASES.stream().anyMatch(lowerText::contains)) {
            throw new UserFacingException("This email looks like a refund, credit, or card payment, so it was not logged.");
        }

        BigDecimal amount = findAmount(text)
                .orElseThrow(() -> new UserFacingException("I could not find a transaction amount in that email."));
        String merchant = findMerchant(text)
                .orElse("Email transaction");
        ExpenseCategory category = inferCategory(lowerText + " " + merchant.toLowerCase(Locale.ROOT));
        LocalDate expenseDate = request.expenseDate() == null
                ? LocalDate.now(clock)
                : request.expenseDate();

        return new ParsedEmailExpense(amount, category, "Auto: " + merchant, expenseDate);
    }

    private Optional<BigDecimal> findAmount(String text) {
        Optional<BigDecimal> currencyAmount = findBestAmount(CURRENCY_AMOUNT, text);
        if (currencyAmount.isPresent()) {
            return currencyAmount;
        }
        return findBestAmount(KEYWORD_AMOUNT, text);
    }

    private Optional<BigDecimal> findBestAmount(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        BigDecimal bestAmount = null;
        int bestScore = Integer.MIN_VALUE;
        while (matcher.find()) {
            BigDecimal amount = parseAmount(matcher);
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            int score = scoreAmountMatch(text, matcher.start(), matcher.end());
            if (score > bestScore) {
                bestAmount = amount;
                bestScore = score;
            }
        }
        return Optional.ofNullable(bestAmount);
    }

    private BigDecimal parseAmount(Matcher matcher) {
        for (int group = 1; group <= matcher.groupCount(); group++) {
            String rawAmount = matcher.group(group);
            if (StringUtils.hasText(rawAmount)) {
                return new BigDecimal(rawAmount.replace(",", ""));
            }
        }
        return null;
    }

    private int scoreAmountMatch(String text, int start, int end) {
        int contextStart = Math.max(0, start - 70);
        int contextEnd = Math.min(text.length(), end + 70);
        String context = text.substring(contextStart, contextEnd).toLowerCase(Locale.ROOT);

        int score = 0;
        if (containsAny(context, "spent", "charged", "paid", "purchase", "transaction amount", "amount debited")) {
            score += 40;
        }
        if (containsAny(context, "balance", "available", "limit", "outstanding", "minimum payment")) {
            score -= 60;
        }
        return score;
    }

    private Optional<String> findMerchant(String text) {
        for (Pattern pattern : MERCHANT_PATTERNS) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String merchant = cleanMerchant(matcher.group(1));
                if (StringUtils.hasText(merchant)) {
                    return Optional.of(merchant);
                }
            }
        }
        return Optional.empty();
    }

    private String cleanMerchant(String rawMerchant) {
        String merchant = rawMerchant
                .replaceAll("(?i)\\s+(on|for|amount|sgd|s\\$|\\$).*$", "")
                .replaceAll("\\s+", " ")
                .trim();
        if (merchant.length() > 60) {
            return merchant.substring(0, 60).trim();
        }
        return merchant;
    }

    private ExpenseCategory inferCategory(String text) {
        if (containsAny(text, "grabfood", "foodpanda", "mcdonald", "starbucks", "restaurant", "cafe", "kopi", "coffee", "hawker", "koufu", "yakun", "kfc")) {
            return ExpenseCategory.FOOD;
        }
        if (containsAny(text, "grab", "gojek", "tada", "taxi", "mrt", "bus", "smrt", "comfortdelgro", "cdg")) {
            return ExpenseCategory.TRANSPORT;
        }
        if (containsAny(text, "netflix", "spotify", "youtube", "apple", "google", "openai", "github", "icloud")) {
            return ExpenseCategory.SUBSCRIPTIONS;
        }
        if (containsAny(text, "shopee", "lazada", "amazon", "uniqlo", "cotton on", "marketplace")) {
            return ExpenseCategory.SHOPPING;
        }
        if (containsAny(text, "cinema", "movie", "klook", "ticket", "game")) {
            return ExpenseCategory.ENTERTAINMENT;
        }
        if (containsAny(text, "clinic", "hospital", "pharmacy", "guardian", "watsons", "doctor")) {
            return ExpenseCategory.HEALTH;
        }
        if (containsAny(text, "nus", "ntu", "smu", "sim", "university", "bookstore", "printing")) {
            return ExpenseCategory.SCHOOL;
        }
        return ExpenseCategory.OTHERS;
    }

    private boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private String combine(String subject, String body) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(subject)) {
            builder.append(subject.trim()).append('\n');
        }
        if (StringUtils.hasText(body)) {
            builder.append(body.trim());
        }
        return builder.toString();
    }
}
