package com.walletlah.email;

import com.walletlah.expense.Expense;
import com.walletlah.user.WalletUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "email_expense_ingests")
public class EmailExpenseIngest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private WalletUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id")
    private Expense expense;

    @Column(name = "source_message_id")
    private String sourceMessageId;

    private String sender;

    private String subject;

    @Column(name = "source_provider")
    private String sourceProvider;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected EmailExpenseIngest() {
    }

    public EmailExpenseIngest(
            WalletUser user,
            Expense expense,
            String sourceMessageId,
            String sender,
            String subject,
            String sourceProvider
    ) {
        this.user = user;
        this.expense = expense;
        this.sourceMessageId = sourceMessageId;
        this.sender = sender;
        this.subject = subject;
        this.sourceProvider = sourceProvider;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
    }
}
