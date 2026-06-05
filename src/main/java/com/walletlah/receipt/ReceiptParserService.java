package com.walletlah.receipt;

import com.walletlah.expense.ExpenseCategory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.textract.model.AnalyzeExpenseResponse;
import software.amazon.awssdk.services.textract.model.ExpenseField;

@Service
public class ReceiptParserService {

    private static final Pattern AMOUNT_PATTERN = Pattern.compile("([0-9][0-9,]*(?:\\.\\d{2})?)");
    private static final List<String> TOTAL_TYPES = List.of("TOTAL", "AMOUNT_DUE", "GRAND_TOTAL");
    private static final List<String> TOTAL_LABELS = List.of("total", "amount due", "grand total", "net total");
    private static final List<String> MERCHANT_TYPES = List.of("VENDOR_NAME", "MERCHANT_NAME", "SUPPLIER_NAME", "RECEIVER_NAME");
    private static final List<String> DATE_TYPES = List.of("INVOICE_RECEIPT_DATE", "RECEIPT_DATE", "DATE");
    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("d-M-yyyy")
    );

    private final ReceiptCategoryInferenceService categoryInferenceService;
    private final ReceiptScanProperties properties;
    private final Clock clock;

    public ReceiptParserService(
            ReceiptCategoryInferenceService categoryInferenceService,
            ReceiptScanProperties properties,
            Clock clock
    ) {
        this.categoryInferenceService = categoryInferenceService;
        this.properties = properties;
        this.clock = clock;
    }

    public ReceiptScanResult parse(AnalyzeExpenseResponse response) {
        List<SummaryField> fields = summaryFields(response);
        String rawText = rawText(fields);
        Optional<SummaryField> amountField = bestAmountField(fields);
        BigDecimal amount = amountField
                .filter(field -> confidentEnough(field.confidence()))
                .flatMap(field -> parseAmount(field.value()))
                .orElse(null);
        BigDecimal confidence = amountField
                .map(SummaryField::confidence)
                .orElse(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
        String merchant = firstValueForTypes(fields, MERCHANT_TYPES)
                .orElseGet(() -> merchantFromRawText(rawText).orElse("Receipt transaction"));
        LocalDate expenseDate = firstValueForTypes(fields, DATE_TYPES)
                .flatMap(this::parseDate)
                .orElse(LocalDate.now(clock));
        ExpenseCategory category = categoryInferenceService.infer(merchant, rawText);

        return new ReceiptScanResult(merchant, amount, expenseDate, category, confidence, rawText);
    }

    private List<SummaryField> summaryFields(AnalyzeExpenseResponse response) {
        List<SummaryField> fields = new ArrayList<>();
        if (response == null || response.expenseDocuments() == null) {
            return fields;
        }

        response.expenseDocuments().forEach(document -> {
            if (document.summaryFields() == null) {
                return;
            }
            document.summaryFields().forEach(field -> {
                String type = field.type() == null ? "" : field.type().text();
                String label = field.labelDetection() == null ? "" : field.labelDetection().text();
                String value = field.valueDetection() == null ? "" : field.valueDetection().text();
                BigDecimal confidence = confidence(field);
                if (StringUtils.hasText(type) || StringUtils.hasText(label) || StringUtils.hasText(value)) {
                    fields.add(new SummaryField(type, label, value, confidence));
                }
            });
        });
        return fields;
    }

    private BigDecimal confidence(ExpenseField field) {
        if (field.valueDetection() != null && field.valueDetection().confidence() != null) {
            return BigDecimal.valueOf(field.valueDetection().confidence());
        }
        if (field.type() != null && field.type().confidence() != null) {
            return BigDecimal.valueOf(field.type().confidence());
        }
        return BigDecimal.ZERO;
    }

    private Optional<SummaryField> bestAmountField(List<SummaryField> fields) {
        List<SummaryField> exactTotals = fields.stream()
                .filter(field -> containsIgnoreCase(TOTAL_TYPES, field.type()))
                .filter(field -> parseAmount(field.value()).isPresent())
                .toList();
        if (!exactTotals.isEmpty()) {
            return exactTotals.stream().max(Comparator.comparing(SummaryField::confidence));
        }

        List<SummaryField> labelledTotals = fields.stream()
                .filter(field -> containsAny(field.label(), TOTAL_LABELS) || containsAny(field.type(), TOTAL_LABELS))
                .filter(field -> parseAmount(field.value()).isPresent())
                .toList();
        if (!labelledTotals.isEmpty()) {
            return labelledTotals.stream().max(Comparator.comparing(SummaryField::confidence));
        }

        List<SummaryField> amountLikeFields = fields.stream()
                .filter(field -> parseAmount(field.value()).isPresent())
                .toList();
        if (amountLikeFields.size() == 1) {
            return Optional.of(amountLikeFields.get(0));
        }
        return Optional.empty();
    }

    private Optional<String> firstValueForTypes(List<SummaryField> fields, List<String> types) {
        return fields.stream()
                .filter(field -> containsIgnoreCase(types, field.type()))
                .filter(field -> StringUtils.hasText(field.value()))
                .max(Comparator.comparing(SummaryField::confidence))
                .map(SummaryField::value);
    }

    private Optional<BigDecimal> parseAmount(String value) {
        if (!StringUtils.hasText(value)) {
            return Optional.empty();
        }
        Matcher matcher = AMOUNT_PATTERN.matcher(value.replace("SGD", "").replace("S$", "").replace("$", ""));
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(new BigDecimal(matcher.group(1).replace(",", "")));
    }

    private Optional<LocalDate> parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return Optional.empty();
        }
        String normalized = value.trim()
                .replace(",", "")
                .replaceAll("\\.$", "");
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return Optional.of(LocalDate.parse(normalized, formatter));
            } catch (DateTimeParseException ignored) {
                // Try the next receipt date format.
            }
        }
        return Optional.empty();
    }

    private Optional<String> merchantFromRawText(String rawText) {
        if (!StringUtils.hasText(rawText)) {
            return Optional.empty();
        }
        return rawText.lines()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .filter(line -> line.length() <= 80)
                .findFirst();
    }

    private String rawText(List<SummaryField> fields) {
        StringBuilder builder = new StringBuilder();
        for (SummaryField field : fields) {
            if (StringUtils.hasText(field.value())) {
                builder.append(field.value()).append('\n');
            }
        }
        return builder.toString().trim();
    }

    private boolean confidentEnough(BigDecimal confidence) {
        return confidence.compareTo(properties.minConfidence()) >= 0;
    }

    private boolean containsIgnoreCase(List<String> values, String candidate) {
        if (!StringUtils.hasText(candidate)) {
            return false;
        }
        return values.stream().anyMatch(value -> value.equalsIgnoreCase(candidate.trim()));
    }

    private boolean containsAny(String text, List<String> values) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        return values.stream().anyMatch(normalized::contains);
    }

    private record SummaryField(String type, String label, String value, BigDecimal confidence) {
    }
}
