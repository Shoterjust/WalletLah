package com.walletlah.receipt;

import com.walletlah.common.UserFacingException;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.AnalyzeExpenseRequest;
import software.amazon.awssdk.services.textract.model.Document;
import software.amazon.awssdk.services.textract.model.TextractException;

@Service
@ConditionalOnProperty(name = "walletlah.receipts.enabled", havingValue = "true")
public class AwsTextractReceiptOcrService implements ReceiptOcrService {

    private static final Logger log = LoggerFactory.getLogger(AwsTextractReceiptOcrService.class);

    private final TextractClient textractClient;
    private final ReceiptParserService receiptParserService;

    public AwsTextractReceiptOcrService(ReceiptScanProperties properties, ReceiptParserService receiptParserService) {
        this.textractClient = TextractClient.builder()
                .region(Region.of(properties.awsRegion()))
                .httpClient(UrlConnectionHttpClient.builder().build())
                .build();
        this.receiptParserService = receiptParserService;
    }

    @Override
    public ReceiptScanResult analyze(byte[] imageBytes) {
        try {
            var request = AnalyzeExpenseRequest.builder()
                    .document(Document.builder()
                            .bytes(SdkBytes.fromByteArray(imageBytes))
                            .build())
                    .build();
            return receiptParserService.parse(textractClient.analyzeExpense(request));
        } catch (TextractException | SdkClientException e) {
            log.error("AWS Textract AnalyzeExpense failed", e);
            throw new UserFacingException("I could not scan that receipt right now. Please try again later or log it manually with /add.");
        }
    }

    @PreDestroy
    void close() {
        textractClient.close();
    }
}
