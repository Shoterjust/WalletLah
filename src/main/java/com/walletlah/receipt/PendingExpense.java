package com.walletlah.receipt;

import com.walletlah.expense.ExpenseCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "pending_expenses")
public class PendingExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "telegram_user_id", nullable = false)
    private Long telegramUserId;

    private String merchant;

    @Column(precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private ExpenseCategory category;

    @Column(name = "expense_date")
    private LocalDate expenseDate;

    @Column(name = "raw_ocr_text")
    private String rawOcrText;

    @Column(name = "receipt_image_file_id")
    private String receiptImageFileId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PendingExpenseSource source = PendingExpenseSource.RECEIPT_SCAN;

    @Column(name = "source_provider")
    private String sourceProvider;

    @Column(name = "source_message_id")
    private String sourceMessageId;

    @Column(name = "source_sender")
    private String sourceSender;

    @Column(name = "source_subject")
    private String sourceSubject;

    @Column(precision = 5, scale = 2)
    private BigDecimal confidence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PendingExpenseStatus status = PendingExpenseStatus.PENDING_CONFIRMATION;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected PendingExpense() {
    }

    public PendingExpense(Long telegramUserId, ReceiptScanResult result, String receiptImageFileId) {
        this.telegramUserId = telegramUserId;
        this.merchant = result.merchant();
        this.amount = result.amount();
        this.category = result.category();
        this.expenseDate = result.expenseDate();
        this.rawOcrText = result.rawOcrText();
        this.receiptImageFileId = receiptImageFileId;
        this.source = PendingExpenseSource.RECEIPT_SCAN;
        this.confidence = result.confidence();
    }

    public PendingExpense(
            Long telegramUserId,
            String merchant,
            BigDecimal amount,
            ExpenseCategory category,
            LocalDate expenseDate,
            String sourceProvider,
            String sourceMessageId,
            String sourceSender,
            String sourceSubject,
            String rawText
    ) {
        this.telegramUserId = telegramUserId;
        this.merchant = merchant;
        this.amount = amount;
        this.category = category;
        this.expenseDate = expenseDate;
        this.source = PendingExpenseSource.EMAIL_INGEST;
        this.sourceProvider = sourceProvider;
        this.sourceMessageId = sourceMessageId;
        this.sourceSender = sourceSender;
        this.sourceSubject = sourceSubject;
        this.rawOcrText = rawText;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public void updateAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void updateCategory(ExpenseCategory category) {
        this.category = category;
    }

    public void updateExpenseDate(LocalDate expenseDate) {
        this.expenseDate = expenseDate;
    }

    public void updateMerchant(String merchant) {
        this.merchant = merchant;
    }

    public void markConfirmed() {
        this.status = PendingExpenseStatus.CONFIRMED;
    }

    public void markCancelled() {
        this.status = PendingExpenseStatus.CANCELLED;
    }

    public void markExpired() {
        this.status = PendingExpenseStatus.EXPIRED;
    }

    public Long getId() {
        return id;
    }

    public Long getTelegramUserId() {
        return telegramUserId;
    }

    public String getMerchant() {
        return merchant;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public ExpenseCategory getCategory() {
        return category;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public String getRawOcrText() {
        return rawOcrText;
    }

    public String getReceiptImageFileId() {
        return receiptImageFileId;
    }

    public PendingExpenseSource getSource() {
        return source;
    }

    public String getSourceProvider() {
        return sourceProvider;
    }

    public String getSourceMessageId() {
        return sourceMessageId;
    }

    public String getSourceSender() {
        return sourceSender;
    }

    public String getSourceSubject() {
        return sourceSubject;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public PendingExpenseStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
