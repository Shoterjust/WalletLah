# WalletLah Dashboard

Next.js dashboard for the WalletLah Telegram expense tracker.

## Local Setup

1. Start the Spring Boot backend on `http://localhost:8080`.
2. Copy the dashboard env file:

```powershell
Copy-Item .env.example .env.local
```

3. Install dependencies:

```powershell
npm install
```

4. Start the dashboard:

```powershell
npm run dev
```

5. Open:

```text
http://localhost:3000
```

## Login Flow

In Telegram, send:

```text
/dashboard_link
```

Enter the 6-digit code in the dashboard. The Next.js app stores the Spring Boot session ID in an HTTP-only dashboard-domain cookie and proxies dashboard requests through Next.js route handlers.

## Environment

```env
WALLETLAH_API_BASE_URL=http://localhost:8080
```

For Vercel, set `WALLETLAH_API_BASE_URL` to the Railway backend URL.

## Implemented MVP Screens

- Telegram link-code login
- Monthly summary cards
- Category breakdown
- Expense table with month/category/source filters
- Add expense
- Edit expense
- Delete expense
- Recurring expense list
- Add recurring expense
- Delete recurring expense
