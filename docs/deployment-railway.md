# Railway Deployment Guide

This guide deploys the WalletLah Telegram bot MVP on Railway with Spring Boot, Docker, and Railway PostgreSQL.

## Prerequisites

- GitHub repository connected to Railway.
- A Telegram bot token from BotFather.
- A Railway project with:
  - One service for the WalletLah app.
  - One PostgreSQL service.

## Required App Variables

Add these variables to the WalletLah app service, not the PostgreSQL service:

```env
TELEGRAM_BOT_ENABLED=true
TELEGRAM_BOT_TOKEN=replace-with-botfather-token
WALLETLAH_ZONE_ID=Asia/Singapore
DASHBOARD_ALLOWED_ORIGINS=https://your-nextjs-dashboard.vercel.app
DASHBOARD_LINK_CODE_TTL_MINUTES=10
SPRING_DATASOURCE_URL=jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}
SPRING_DATASOURCE_USERNAME=${{Postgres.PGUSER}}
SPRING_DATASOURCE_PASSWORD=${{Postgres.PGPASSWORD}}
```

If the PostgreSQL service has a different Railway service name, replace `Postgres` in the variable references with that service name.

Optional email ingest:

```env
EMAIL_INGEST_ENABLED=true
EMAIL_INGEST_TOKEN=replace-with-long-random-secret
```

Optional receipt scanning:

```env
RECEIPT_SCAN_ENABLED=true
AWS_REGION=ap-southeast-1
AWS_ACCESS_KEY_ID=replace-with-aws-access-key
AWS_SECRET_ACCESS_KEY=replace-with-aws-secret-key
RECEIPT_SCAN_MAX_FILE_SIZE_BYTES=5000000
RECEIPT_SCAN_MIN_CONFIDENCE=70
RECEIPT_PENDING_EXPIRY_MINUTES=30
RECEIPT_SCAN_RATE_LIMIT_SECONDS=20
```

## Build Settings

Railway can build this repository directly from the `Dockerfile`.

Expected build flow:

```text
mvn -DskipTests dependency:go-offline
mvn -DskipTests package
java -jar /app/walletlah.jar
```

The app listens on `${PORT}` through Spring Boot:

```yaml
server:
  port: ${PORT:8080}
```

## Health Check

After deployment, open the generated Railway app URL:

```text
https://your-walletlah-service.up.railway.app/actuator/health
```

Expected response:

```json
{"status":"UP"}
```

## Telegram Smoke Test

Open Telegram and send these messages to the bot:

```text
/start
/dashboard_link
/budget 600
/add 5.50 food chicken rice
/status
/recent
/recent 10
/categories
/edit_latest amount 6.20
/recurring_add 14.99 subscriptions Spotify monthly
/recurring
```

Expected behavior:

- `/start` returns the welcome message.
- `/dashboard_link` returns a 6-digit dashboard login code.
- `/budget 600` saves this month's budget and returns a status summary.
- `/add` creates a manual expense.
- `/status` shows total spent, average daily spend, remaining budget, safe daily spend, and budget-used percentage.
- `/recent` shows expense IDs that can be edited.
- `/recurring_add` creates a recurring rule.

## Common Failures

### Bot does not reply

Check Railway variables:

```text
TELEGRAM_BOT_ENABLED=true
TELEGRAM_BOT_TOKEN=<actual token>
```

Also confirm no local copy of the bot is running with the same token. Telegram long polling allows only one active polling consumer per bot token.

### App starts but database fails

Check that the app service variables reference the PostgreSQL service variables:

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}
SPRING_DATASOURCE_USERNAME=${{Postgres.PGUSER}}
SPRING_DATASOURCE_PASSWORD=${{Postgres.PGPASSWORD}}
```

If Railway logs show connection refused or unknown host, the service reference name is likely wrong.

### Flyway migration fails

Do not manually edit old migration files after they have run in Railway. Add a new `V5__...sql` migration instead.

### Telegram 404 from API calls

A Telegram `404 Not Found` usually means the URL is malformed or the token is wrong. Use BotFather to copy the token again and paste it into `TELEGRAM_BOT_TOKEN`.

## Deployment Checklist

- Railway app service is connected to GitHub `main`.
- PostgreSQL service is added.
- App service variables are configured.
- `TELEGRAM_BOT_ENABLED=true`.
- `TELEGRAM_BOT_TOKEN` is valid.
- `DASHBOARD_ALLOWED_ORIGINS` matches the deployed Next.js dashboard URL.
- `/actuator/health` returns `UP`.
- Telegram `/start` replies.
- `/budget`, `/add`, `/status`, `/recent`, and `/recurring_add` have been smoke-tested.

## Next.js Dashboard Deployment

Deploy the `dashboard/` directory as a separate Vercel project.

Vercel project settings:

```text
Root Directory: dashboard
Build Command: npm run build
Output Directory: .next
Install Command: npm install
```

Vercel environment variable:

```env
WALLETLAH_API_BASE_URL=https://your-walletlah-service.up.railway.app
```

After Vercel deploys, copy the Vercel app URL into the Railway backend variable:

```env
DASHBOARD_ALLOWED_ORIGINS=https://your-walletlah-dashboard.vercel.app
```
