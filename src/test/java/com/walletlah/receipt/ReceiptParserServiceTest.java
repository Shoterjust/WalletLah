package com.walletlah.receipt;

import static org.assertj.core.api.Assertions.assertThat;

import com.walletlah.expense.ExpenseCategory;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.textract.model.AnalyzeExpenseResponse;
import software.amazon.awssdk.services.textract.model.ExpenseDetection;
import software.amazon.awssdk.services.textract.model.ExpenseDocument;
import software.amazon.awssdk.services.textract.model.ExpenseField;
import software.amazon.awssdk.services.textract.model.ExpenseType;

class ReceiptParserServiceTest {

    private final ReceiptParserService parser = new ReceiptParserService(
            new ReceiptCategoryInferenceService(),
            new ReceiptScanProperties(true, "ap-southeast-1", 5_000_000, new BigDecimal("70"), 30, 20),
            Clock.fixed(Instant.parse("2026-06-05T04:00:00Z"), ZoneId.of("Asia/Singapore"))
    );

    @Test
    void parsesTextractSummaryFields() {
        ReceiptScanResult result = parser.parse(AnalyzeExpenseResponse.builder()
                .expenseDocuments(ExpenseDocument.builder()
                        .summaryFields(
                                field("VENDOR_NAME", "Koufu", 98),
                                field("TOTAL", "S$6.80", 97),
                                field("INVOICE_RECEIPT_DATE", "05 Jun 2026", 95)
                        )
                        .build())
                .build());

        assertThat(result.merchant()).isEqualTo("Koufu");
        assertThat(result.amount()).isEqualByComparingTo("6.80");
        assertThat(result.expenseDate()).isEqualTo("2026-06-05");
        assertThat(result.category()).isEqualTo(ExpenseCategory.FOOD);
        assertThat(result.confidence()).isEqualByComparingTo("97.00");
    }

    @Test
    void asksForManualAmountWhenTotalConfidenceIsLow() {
        ReceiptScanResult result = parser.parse(AnalyzeExpenseResponse.builder()
                .expenseDocuments(ExpenseDocument.builder()
                        .summaryFields(
                                field("VENDOR_NAME", "Koufu", 98),
                                field("TOTAL", "S$6.80", 50)
                        )
                        .build())
                .build());

        assertThat(result.amount()).isNull();
        assertThat(result.confidence()).isEqualByComparingTo("50.00");
    }

    private ExpenseField field(String type, String value, float confidence) {
        return ExpenseField.builder()
                .type(ExpenseType.builder()
                        .text(type)
                        .confidence(confidence)
                        .build())
                .valueDetection(ExpenseDetection.builder()
                        .text(value)
                        .confidence(confidence)
                        .build())
                .build();
    }
}
