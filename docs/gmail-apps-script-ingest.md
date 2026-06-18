# Gmail Auto-Logging With Google Apps Script

This guide connects Gmail transaction emails to WalletLah without building native Gmail OAuth into the Spring Boot app.

The workflow is:

1. Gmail receives a bank/card transaction email.
2. Google Apps Script polls Gmail on a timer.
3. The script sends matching messages to WalletLah's `/api/email-expenses` endpoint.
4. WalletLah parses the email and sends a Telegram confirmation.
5. The user replies `YES`, `NO`, or edits the pending expense in Telegram.
6. WalletLah only saves the expense after Telegram confirmation.

## Prerequisites

- WalletLah backend deployed on Railway.
- Telegram bot is working in production.
- Email ingest enabled on the Railway app service.
- The Gmail address is linked to your WalletLah Telegram account.

In Telegram:

```text
/email you@gmail.com
```

On the Railway WalletLah app service, set:

```env
EMAIL_INGEST_ENABLED=true
EMAIL_INGEST_TOKEN=replace-with-a-long-random-secret
TELEGRAM_BOT_ENABLED=true
```

Keep the token private. The Google Apps Script uses the same value as `WALLETLAH_INGEST_TOKEN`.

## Add The Google Apps Script

1. Open [script.google.com](https://script.google.com).
2. Create a new project.
3. Rename it to `WalletLah Gmail Ingest`.
4. Replace the default `Code.gs` content with:

```text
integrations/google-apps-script/walletlah-gmail-ingest.gs
```

Use the file from this repository and paste its full content into Apps Script.

## Configure Script Properties

In Apps Script:

1. Open `Project Settings`.
2. Under `Script Properties`, add these keys:

```text
WALLETLAH_API_BASE_URL=https://your-railway-service.up.railway.app
WALLETLAH_INGEST_TOKEN=the-same-value-as-EMAIL_INGEST_TOKEN
WALLETLAH_RECIPIENT_EMAIL=you@gmail.com
```

Do not include `/api/email-expenses` in `WALLETLAH_API_BASE_URL`. The script appends that path.

Optional properties:

```text
WALLETLAH_MAX_THREADS=20
WALLETLAH_BODY_MAX_CHARS=12000
WALLETLAH_MARK_REJECTED_PROCESSED=true
WALLETLAH_PROCESSED_LABEL=WalletLahProcessed
WALLETLAH_REJECTED_LABEL=WalletLahRejected
WALLETLAH_ERROR_LABEL=WalletLahError
```

Optional custom Gmail query:

```text
WALLETLAH_GMAIL_QUERY=newer_than:30d {spent transaction purchase card charged debited receipt DBS OCBC UOB Trust YouTrip Revolut}
```

Start broad, then tighten the query after checking your own bank email formats.

## Install The Timer

In Apps Script:

1. Select `setupWalletLahGmailIngest`.
2. Click `Run`.
3. Approve Gmail and external request permissions.

This creates:

- `WalletLahProcessed`
- `WalletLahRejected`
- `WalletLahError`
- A 15-minute timer trigger for `runWalletLahGmailIngest`

If you accidentally run setup more than once, it will not create duplicate triggers for the same handler.

## Test The Connection

Run this function manually:

```text
testWalletLahEmailIngest
```

Expected result:

1. Apps Script logs an HTTP `200`.
2. Telegram receives a pending test transaction.
3. Reply `NO` in Telegram to cancel the test transaction, or `YES` if you want to save it.

Then run:

```text
runWalletLahGmailIngest
```

Expected result:

1. Apps Script scans recent matching Gmail threads.
2. Matching messages are posted to Railway.
3. Telegram receives pending confirmations.
4. Gmail threads get one of these labels:
   - `WalletLahProcessed`
   - `WalletLahRejected`
   - `WalletLahError`

The script also stores handled Gmail message IDs in Apps Script properties, so repeated timer runs do not send the same Gmail message again.

## Production Workflow

Use this daily flow:

1. Pay by card or wallet.
2. Bank/payment provider sends a transaction email to Gmail.
3. Apps Script picks it up within 15 minutes.
4. Telegram sends a pending WalletLah transaction.
5. Reply:

```text
YES
```

or correct it first:

```text
amount 8.90
category food
date 2026-06-18
merchant Koufu
YES
```

Use:

```text
NO
```

for emails that should not become expenses.

## Troubleshooting

`401` from WalletLah:

- `WALLETLAH_INGEST_TOKEN` does not match Railway `EMAIL_INGEST_TOKEN`.

`404` from WalletLah:

- `WALLETLAH_API_BASE_URL` is wrong, or `EMAIL_INGEST_ENABLED` is not `true`.
- Use the Railway backend URL, not the Vercel dashboard URL.

`400` from WalletLah:

- The Gmail address is not linked in Telegram with `/email`.
- The parser could not find a transaction amount.
- The email looked like a refund, cashback, reversal, or card payment.

No Telegram message:

- Confirm `TELEGRAM_BOT_ENABLED=true` on Railway.
- Confirm the user has started the bot with `/start`.
- Confirm the linked email exactly matches `WALLETLAH_RECIPIENT_EMAIL`.
- Check Railway logs for `/api/email-expenses`.

Repeated messages:

- Confirm Apps Script properties are being saved.
- Confirm `messageId` is included in requests.
- Check whether you copied the latest `walletlah-gmail-ingest.gs`.

Too many irrelevant emails:

- Tighten `WALLETLAH_GMAIL_QUERY`.
- Example:

```text
newer_than:30d {from:dbs.com from:ocbc.com from:uobgroup.com from:trustbank.sg from:you.co from:revolut.com}
```

Apps Script logs are under:

```text
Apps Script > Executions
```

Open the failed execution to see the exact HTTP status and response body.

## Why This Is The MVP Approach

Google Apps Script is the fastest reliable implementation for the current product stage:

- No native Gmail OAuth screens.
- No token storage in WalletLah.
- No background worker service needed.
- Confirmation still happens in Telegram before saving.

Native Gmail OAuth can be added later if the project needs multi-user self-service onboarding.
