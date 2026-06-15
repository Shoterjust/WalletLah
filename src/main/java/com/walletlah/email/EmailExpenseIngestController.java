package com.walletlah.email;

import com.walletlah.common.UserFacingException;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/email-expenses")
@ConditionalOnProperty(name = "walletlah.email-ingest.enabled", havingValue = "true")
public class EmailExpenseIngestController {

    private final EmailIngestProperties properties;
    private final EmailExpenseIngestService ingestService;

    public EmailExpenseIngestController(EmailIngestProperties properties, EmailExpenseIngestService ingestService) {
        this.properties = properties;
        this.ingestService = ingestService;
    }

    @PostMapping
    public ResponseEntity<EmailExpenseIngestResponse> ingest(
            @RequestHeader(value = "X-WalletLah-Ingest-Token", required = false) String token,
            @Valid @RequestBody EmailExpenseIngestRequest request
    ) {
        if (!properties.matchesToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(EmailExpenseIngestResponse.rejected("Invalid email ingest token."));
        }

        try {
            EmailExpenseIngestResult result = ingestService.ingest(request);
            if (result.duplicate()) {
                return ResponseEntity.ok(EmailExpenseIngestResponse.duplicate(result.message()));
            }
            return ResponseEntity.ok(EmailExpenseIngestResponse.pending(result.pendingExpense()));
        } catch (UserFacingException e) {
            return ResponseEntity.badRequest().body(EmailExpenseIngestResponse.rejected(e.getMessage()));
        }
    }
}
