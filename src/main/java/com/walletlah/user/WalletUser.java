package com.walletlah.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "wallet_users")
public class WalletUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "telegram_user_id", nullable = false, unique = true)
    private Long telegramUserId;

    @Column(name = "telegram_chat_id", nullable = false)
    private Long telegramChatId;

    private String username;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "email_address")
    private String emailAddress;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WalletUser() {
    }

    public WalletUser(Long telegramUserId, Long telegramChatId, String username, String firstName) {
        this.telegramUserId = telegramUserId;
        this.telegramChatId = telegramChatId;
        this.username = username;
        this.firstName = firstName;
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

    public void updateTelegramProfile(Long telegramChatId, String username, String firstName) {
        this.telegramChatId = telegramChatId;
        this.username = username;
        this.firstName = firstName;
    }

    public void updateEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public Long getId() {
        return id;
    }

    public Long getTelegramUserId() {
        return telegramUserId;
    }

    public Long getTelegramChatId() {
        return telegramChatId;
    }

    public String getUsername() {
        return username;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
