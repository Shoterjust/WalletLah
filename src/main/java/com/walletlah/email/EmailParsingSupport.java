package com.walletlah.email;

import com.walletlah.common.UserFacingException;
import com.walletlah.expense.ExpenseCategory;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

final class EmailParsingSupport {

    private static final Pattern CURRENCY_AMOUNT = Pattern.compile(
            "(?i)(?:SGD|S\\$|\\$)\\s*([0-9][0-9,]*(?:\\.\\d{2})?)|([0-9][0-9,]*(?:\\.\\d{2})?)\\s*(?:SGD)"
    );
    private static final Pattern KEYWORD_AMOUNT = Pattern.compile(
            "(?i)(?:amount|transaction amount|spent|charged|paid|purchase|debited)\\D{0,30}([0-9][0-9,]*\\.\\d{2})"
    );
    private static final Pattern ISO_DATE = Pattern.compile("\\b(20\\d{2}-\\d{2}-\\d{2})\\b");
    private static final Pattern SLASH_DATE = Pattern.compile("\\b(\\d{1,2}/\\d{1,2}/20\\d{2})\\b");
    private static final Pattern MONTH_DATE = Pattern.compile("\\b(\\d{1,2}\\s+[A-Za-z]{3,9}\\s+20\\d{2})\\b");
    private static final DateTimeFormatter MONTH_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("d MMM yyyy")
            .toFormatter(Locale.ENGLISH);
    private static final DateTimeFormatter LONG_MONTH_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("d MMMM yyyy")
            .toFormatter(Locale.ENGLISH);
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
            Pattern.compile("(?im)(?:merchant|description|transaction at|purchase at|spent at|charged at|paid to|made at)\\s*[:\\-]?\\s*([^\\n\\r.]{2,80})"),
            Pattern.compile("(?im)\\bat\\s+([^\\n\\r.]{2,80})")
    );

    private EmailParsingSupport() {
    }

    static void rejectNonExpense(String text) {
        String lowerText = text.toLowerCase(Locale.ROOT);
        if (NON_EXPENSE_PHRASES.stream().anyMatch(lowerText::contains)) {
            throw new UserFacingException("This email looks like a refund, credit, or card payment, so it was not logged.");
        }
    }

    static Optional<BigDecimal> findAmount(String text) {
        Optional<BigDecimal> currencyAmount = findBestAmount(CURRENCY_AMOUNT, text);
        if (currencyAmount.isPresent()) {
            return currencyAmount;
        }
        return findBestAmount(KEYWORD_AMOUNT, text);
    }

    static Optional<String> findMerchant(String text) {
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

    static LocalDate findDateOrDefault(EmailExpenseIngestRequest request, Clock clock) {
        if (request.expenseDate() != null) {
            return request.expenseDate();
        }

        String text = combine(request.subject(), request.body());
        Optional<LocalDate> parsedDate = parseFirst(ISO_DATE, text, LocalDate::parse);
        if (parsedDate.isPresent()) {
            return parsedDate.get();
        }
        parsedDate = parseFirst(SLASH_DATE, text, value -> {
            String[] parts = value.split("/");
            return LocalDate.of(
                    Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[0])
            );
        });
        if (parsedDate.isPresent()) {
            return parsedDate.get();
        }
        parsedDate = parseFirst(MONTH_DATE, text, value -> parseMonthDate(value)
                .orElseThrow(() -> new DateTimeParseException("Invalid date", value, 0)));
        return parsedDate.orElseGet(() -> LocalDate.now(clock));
    }

    static ExpenseCategory inferCategory(String text) {
        String lowerText = text.toLowerCase(Locale.ROOT);
        if (containsAny(lowerText, "grabfood", "foodpanda", "mcdonald", "starbucks", "restaurant", "cafe", "kopi", "coffee", "hawker", "koufu", "yakun", "kfc")) {
            return ExpenseCategory.FOOD;
        }
        if (containsAny(lowerText, "grab", "gojek", "tada", "taxi", "mrt", "bus", "smrt", "comfortdelgro", "cdg")) {
            return ExpenseCategory.TRANSPORT;
        }
        if (containsAny(lowerText, "netflix", "spotify", "youtube", "apple", "google", "openai", "github", "icloud")) {
            return ExpenseCategory.SUBSCRIPTIONS;
        }
        if (containsAny(lowerText, "shopee", "lazada", "amazon", "uniqlo", "cotton on", "marketplace", "fairprice", "cold storage")) {
            return ExpenseCategory.SHOPPING;
        }
        if (containsAny(lowerText, "cinema", "movie", "klook", "ticket", "game")) {
            return ExpenseCategory.ENTERTAINMENT;
        }
        if (containsAny(lowerText, "clinic", "hospital", "pharmacy", "guardian", "watsons", "doctor")) {
            return ExpenseCategory.HEALTH;
        }
        if (containsAny(lowerText, "nus", "ntu", "smu", "sim", "university", "bookstore", "printing")) {
            return ExpenseCategory.SCHOOL;
        }
        return ExpenseCategory.OTHERS;
    }

    static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    static String combine(String subject, String body) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(subject)) {
            builder.append(subject.trim()).append('\n');
        }
        if (StringUtils.hasText(body)) {
            builder.append(body.trim());
        }
        return builder.toString();
    }

    static String cleanMerchant(String rawMerchant) {
        String merchant = rawMerchant
                .replaceAll("(?i)\\s+(on|for|amount|sgd|s\\$|\\$|card|transaction).*$", "")
                .replaceAll("\\s+", " ")
                .trim();
        if (merchant.length() > 60) {
            return merchant.substring(0, 60).trim();
        }
        return merchant;
    }

    private static Optional<BigDecimal> findBestAmount(Pattern pattern, String text) {
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

    private static BigDecimal parseAmount(Matcher matcher) {
        for (int group = 1; group <= matcher.groupCount(); group++) {
            String rawAmount = matcher.group(group);
            if (StringUtils.hasText(rawAmount)) {
                return new BigDecimal(rawAmount.replace(",", ""));
            }
        }
        return null;
    }

    private static int scoreAmountMatch(String text, int start, int end) {
        int contextStart = Math.max(0, start - 70);
        int contextEnd = Math.min(text.length(), end + 70);
        String context = text.substring(contextStart, contextEnd).toLowerCase(Locale.ROOT);

        int score = 0;
        if (containsAny(context, "spent", "charged", "paid", "purchase", "transaction amount", "amount debited", "debited")) {
            score += 40;
        }
        if (containsAny(context, "balance", "available", "limit", "outstanding", "minimum payment")) {
            score -= 60;
        }
        return score;
    }

    private static <T> Optional<T> parseFirst(Pattern pattern, String text, Parser<T> parser) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            try {
                return Optional.of(parser.parse(matcher.group(1)));
            } catch (RuntimeException ignored) {
            }
        }
        return Optional.empty();
    }

    private static Optional<LocalDate> parseMonthDate(String value) {
        try {
            return Optional.of(LocalDate.parse(value, MONTH_FORMATTER));
        } catch (DateTimeParseException ignored) {
            try {
                return Optional.of(LocalDate.parse(value, LONG_MONTH_FORMATTER));
            } catch (DateTimeParseException ignoredAgain) {
                return Optional.empty();
            }
        }
    }

    private interface Parser<T> {
        T parse(String value);
    }
}
