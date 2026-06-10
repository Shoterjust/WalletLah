# WalletLah

WalletLah is a Telegram-first expense tracker for Singapore university students. The MVP focuses on logging expenses in a few seconds, checking monthly spending, and knowing a safe daily spend for the rest of the month.

## MVP Commands

```text
/start
/help
/add 5.50 food chicken rice
/status
/recent
/edit 12 amount 7.20
/edit_latest category food
/budget 600
/email you@example.com
/recurring_add 14.99 subscriptions Spotify monthly
/recurring
/recurring_delete 3
/delete_latest
/category food
/categories
```

You can also send a receipt photo. WalletLah scans it, creates a pending expense, and asks for confirmation before saving.

Fast expense logging also accepts:

```text
5.50 food chicken rice
food 5.50 chicken rice
```

## Local Setup

1. Create a Telegram bot with BotFather and copy the token.
2. Copy `.env.example` to `.env`.
3. Replace `TELEGRAM_BOT_TOKEN`.
4. Start Docker Desktop.
5. Start the app and PostgreSQL:

```powershell
docker compose up --build
```

The bot uses long polling for the MVP, so no public HTTPS URL is needed for local development.

## Editing Expenses

`/recent` shows database IDs so specific expenses can be edited:

```text
Recent expenses:
1. #12 2026-06-10 - S$5.50 Food - chicken rice
```

Edit by ID:

```text
/edit 12 amount 7.20
/edit 12 category food
/edit 12 description chicken rice add egg
/edit 12 date 2026-06-05
/edit 12 merchant Koufu
```

Edit the most recently created expense:

```text
/edit_latest amount 7.20
/edit_latest category transport
```

Editable fields:

- `amount`
- `category`
- `description`
- `date`
- `merchant`

After an edit, WalletLah returns the updated expense and refreshed monthly status.

## Recurring Expenses

Recurring expenses generate normal expenses automatically. The scheduler runs daily at `00:05` Singapore time.

Add a recurring expense:

```text
/recurring_add 14.99 subscriptions Spotify monthly
/recurring_add 80 transport concession monthly 2026-07-01
```

Format:

```text
/recurring_add <amount> <category> <description> <daily|weekly|monthly> [next-run-date]
```

If `next-run-date` is omitted, WalletLah starts from today.

List active recurring expenses:

```text
/recurring
```

Cancel one:

```text
/recurring_delete 3
```

Generated recurring expenses appear in `/recent` with `[recurring]`.

## Receipt Photo Scanning

WalletLah can scan receipt photos with AWS Textract AnalyzeExpense. The bot never saves a scanned receipt automatically. It first stores extracted fields as a pending expense and asks the user to confirm.

Telegram flow:

1. Send a receipt photo to the bot.
2. WalletLah downloads the highest-resolution Telegram photo.
3. WalletLah sends the image bytes to AWS Textract AnalyzeExpense.
4. WalletLah extracts merchant, amount, receipt date, category, and confidence.
5. WalletLah replies with a confirmation message.
6. Reply `YES` to save, `NO` to cancel, or edit fields before saving.

Example response:

```text
Receipt scanned.

Merchant: Koufu
Amount: S$6.80
Date: 2026-06-05
Category: Food
Confidence: 98.00%

Reply YES to save, NO to cancel, or edit using:
amount 7.20
category food
date 2026-06-05
merchant Koufu
```

Supported pending receipt replies:

```text
YES
NO
amount 7.20
category food
date 2026-06-05
merchant Koufu
```

If the amount is missing or below the confidence threshold, WalletLah keeps the receipt pending and asks you to enter the amount manually. If OCR fails completely, the bot shows a friendly error and suggests manual logging with `/add`.

Enable receipt scanning in `.env`:

```env
RECEIPT_SCAN_ENABLED=true
AWS_REGION=ap-southeast-1
AWS_ACCESS_KEY_ID=replace-with-access-key
AWS_SECRET_ACCESS_KEY=replace-with-secret-key
RECEIPT_SCAN_MAX_FILE_SIZE_BYTES=5000000
RECEIPT_SCAN_MIN_CONFIDENCE=70
RECEIPT_PENDING_EXPIRY_MINUTES=30
RECEIPT_SCAN_RATE_LIMIT_SECONDS=20
```

Recommended AWS setup:

1. Create an IAM user or role for local development.
2. Grant only the Textract action needed for this MVP:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": "textract:AnalyzeExpense",
      "Resource": "*"
    }
  ]
}
```

3. Put credentials in environment variables or your AWS profile. Do not commit AWS keys.
4. Add an AWS billing alert before testing with many receipts.

Telegram Bot API methods used:

- `getFile` to turn the receipt photo `file_id` into a Telegram file path.
- `https://api.telegram.org/file/bot<TOKEN>/<file_path>` to download the temporary file bytes.

Security and cost controls:

- Receipt scan processing is disabled by default.
- Maximum image size defaults to 5 MB.
- Only Telegram photos are processed for now, not arbitrary documents.
- A simple per-user rate limit defaults to 20 seconds.
- Receipt images are not stored locally after processing.
- Only Telegram `file_id` is stored with the pending expense and final expense.
- Pending receipt entries expire after 30 minutes by default.

Known limitations:

- Blurry, cropped, handwritten, or very long receipts may need manual edits.
- Category inference is rule-based and intentionally simple.
- The MVP does not yet upload receipt images to S3.
- The MVP does not yet include a background cleanup job for expired pending rows.

## Email Receipt Auto-Logging

WalletLah can auto-log expenses from forwarded receipt or card transaction emails.

MVP flow:

1. Link your email in Telegram:

```text
/email you@example.com
```

2. Enable the email ingest endpoint in `.env`:

```env
EMAIL_INGEST_ENABLED=true
EMAIL_INGEST_TOKEN=replace-with-a-long-random-secret
```

3. Start the app:

```powershell
docker compose up --build
```

4. Send a test transaction email payload:

```bash
curl -X POST http://localhost:8080/api/email-expenses \
  -H "Content-Type: application/json" \
  -H "X-WalletLah-Ingest-Token: replace-with-a-long-random-secret" \
  -d '{
    "recipientEmail": "you@example.com",
    "sender": "alerts@bank.example",
    "subject": "Card transaction alert",
    "body": "You have spent SGD 12.30 at MCDONALD'\''S on 05 Jun 2026. Available balance: SGD 800.00",
    "messageId": "demo-message-1"
  }'
```

5. Check Telegram:

```text
/recent
```

The endpoint is intended to be called by an email automation tool such as Make, Zapier, or Google Apps Script. The automation should send the original recipient email, subject, body, sender, and a stable message ID. WalletLah uses the message ID to avoid logging the same email twice.

For production, the app must be deployed at a public HTTPS URL before an email automation service can call `/api/email-expenses`.

## Run Without Telegram

By default, `TELEGRAM_BOT_ENABLED` can be set to `false`. The Spring Boot app will still start, run migrations, and expose health checks:

```text
GET http://localhost:8080/actuator/health
```

## Tests

If Maven is installed:

```powershell
mvn test
```

If Maven is not installed, Docker can still build the project once Docker Desktop is running:

```powershell
docker compose build app
```

## Database

Flyway creates:

- `wallet_users`
- `expenses`
- `monthly_budgets`
- `email_expense_ingests`
- `pending_expenses`
- `recurring_expenses`

Telegram user ID is the MVP identity. A separate login system can be added later if WalletLah grows into a web dashboard.

Receipt scanning adds these expense fields:

- `merchant`
- `source`, such as `MANUAL`, `RECEIPT_SCAN`, or `RECURRING`
- `receipt_image_file_id`

Recurring expenses add:

- `recurring_expenses`
- `expenses.recurring_expense_id`
- `source = RECURRING` for generated rows

Pending receipt statuses:

- `PENDING_CONFIRMATION`
- `CONFIRMED`
- `CANCELLED`
- `EXPIRED`

## Project Structure

```text
src/main/java/com/walletlah
  analytics/   Monthly totals, safe daily spend, category breakdown
  bot/         Telegram bot, command router, response formatting
  budget/      Monthly budget entity, repository, service
  common/      Shared money/date/error helpers
  email/       Receipt and card email ingest endpoint and parser
  expense/     Expense entity, parser, repository, service
  recurring/   Recurring expense rules and scheduler
  receipt/     Receipt photo OCR, category inference, pending confirmation
  user/        Telegram user entity, repository, service
```

Receipt scanning classes:

- `TelegramUpdateHandler`: routes Telegram updates by message type.
- `ReceiptPhotoHandler`: handles receipt photo messages.
- `TelegramFileService`: downloads Telegram files by `file_id`.
- `ReceiptOcrService`: provider abstraction for receipt OCR.
- `AwsTextractReceiptOcrService`: AWS Textract AnalyzeExpense implementation.
- `ReceiptParserService`: extracts merchant, total, date, category, and confidence.
- `PendingExpenseService`: creates, edits, confirms, cancels, and expires pending expenses.
- `ExpenseService`: saves confirmed receipt scans into the normal expenses table.

## Receipt Scanning Roadmap

Phase 1: pending expense table and service.
Phase 2: Telegram photo upload and download.
Phase 3: AWS Textract AnalyzeExpense integration.
Phase 4: receipt field parsing and category inference.
Phase 5: confirmation, edit, and cancel workflow.
Phase 6: cleanup jobs, more tests, and production deployment notes.

## Receipt Testing Checklist

- Unit test receipt field parsing from Textract summary fields.
- Unit test category inference for food, transport, groceries, school, and subscriptions.
- Integration test confirming a pending receipt into a normal expense.
- Manual test with clear food court, supermarket, and transport receipts.
- Manual test with blurry receipts, missing totals, and multiple total-like values.
- Manual test `YES` with no pending expense.
- Manual test invalid edits such as `amount abc` and `date tomorrow`.

## Sprint Testing Checklist

Editing:

- Add an expense with `/add 5.50 food chicken rice`.
- Run `/recent` and note the `#id`.
- Run `/edit <id> amount 7.20`.
- Run `/edit_latest category transport`.
- Run `/status` and verify totals changed.

Recurring:

- Run `/recurring_add 14.99 subscriptions Spotify monthly`.
- Run `/recurring` and verify the rule appears.
- Run `/recurring_delete <id>` and verify it disappears from `/recurring`.
- Add a rule with today's date and verify the scheduler generates it after midnight, or temporarily test generation through service tests during development.

## Portfolio Notes

Resume line:

```text
Built WalletLah, a Telegram-first expense tracker using Spring Boot, PostgreSQL, Flyway, Docker, and the Telegram Bot API. Implemented expense logging, monthly budgets, recent transactions, category analytics, and safe daily spend calculations.
```

Good demo screenshots:

- `/start`
- `/add 5.50 food chicken rice`
- `/status`
- `/recent`
- `/categories`
- `/delete_latest`

Technical decisions to mention:

- Telegram-first UX because daily expense logging must be fast.
- PostgreSQL for durable relational transaction storage.
- Flyway for versioned schema changes.
- `BigDecimal` for money.
- Long polling for MVP simplicity, webhook later for production HTTPS deployment.
