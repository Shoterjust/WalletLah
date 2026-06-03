# WalletLah

WalletLah is a Telegram-first expense tracker for Singapore university students. The MVP focuses on logging expenses in a few seconds, checking monthly spending, and knowing a safe daily spend for the rest of the month.

## MVP Commands

```text
/start
/help
/add 5.50 food chicken rice
/status
/recent
/budget 600
/delete_latest
/category food
/categories
```

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

Telegram user ID is the MVP identity. A separate login system can be added later if WalletLah grows into a web dashboard.

## Project Structure

```text
src/main/java/com/walletlah
  analytics/   Monthly totals, safe daily spend, category breakdown
  bot/         Telegram bot, command router, response formatting
  budget/      Monthly budget entity, repository, service
  common/      Shared money/date/error helpers
  expense/     Expense entity, parser, repository, service
  user/        Telegram user entity, repository, service
```

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
