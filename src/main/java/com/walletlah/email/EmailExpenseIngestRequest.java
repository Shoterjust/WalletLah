package com.walletlah.email;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record EmailExpenseIngestRequest(
        @NotBlank @Email String recipientEmail,
        String sender,
        String subject,
        @NotBlank String body,
        String messageId,
        LocalDate expenseDate
) {
}
