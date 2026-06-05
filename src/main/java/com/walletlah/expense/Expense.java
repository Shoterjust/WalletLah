package com.walletlah.expense;

import com.walletlah.user.WalletUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "expenses")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private WalletUser user;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpenseCategory category;

    private String description;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    private String merchant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpenseSource source = ExpenseSource.MANUAL;

    @Column(name = "receipt_image_file_id")
    private String receiptImageFileId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Expense() {
    }

    public Expense(WalletUser user, BigDecimal amount, ExpenseCategory category, String description, LocalDate expenseDate) {
        this.user = user;
        this.amount = amount;
        this.category = category;
        this.description = description;
        this.expenseDate = expenseDate;
        this.source = ExpenseSource.MANUAL;
    }

    public Expense(
            WalletUser user,
            BigDecimal amount,
            ExpenseCategory category,
            String description,
            LocalDate expenseDate,
            String merchant,
            ExpenseSource source,
            String receiptImageFileId
    ) {
        this.user = user;
        this.amount = amount;
        this.category = category;
        this.description = description;
        this.expenseDate = expenseDate;
        this.merchant = merchant;
        this.source = source;
        this.receiptImageFileId = receiptImageFileId;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public WalletUser getUser() {
        return user;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public ExpenseCategory getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public String getMerchant() {
        return merchant;
    }

    public ExpenseSource getSource() {
        return source;
    }

    public String getReceiptImageFileId() {
        return receiptImageFileId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
