import type {
  DashboardSummary,
  DashboardUser,
  Expense,
  ExpenseForm,
  PageResponse,
  RecurringExpense,
  RecurringForm,
} from "./types";

export class ApiError extends Error {
  constructor(message: string, public readonly status: number) {
    super(message);
  }
}

async function requestJson<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  if (init.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(path, {
    ...init,
    headers,
    credentials: "include",
  });

  const text = await response.text();
  const data = parseJson(text);
  if (!response.ok) {
    throw new ApiError(data?.message ?? "Request failed.", response.status);
  }
  return data as T;
}

function parseJson(text: string) {
  if (!text) {
    return null;
  }
  try {
    return JSON.parse(text);
  } catch {
    return { message: "Unexpected response from WalletLah." };
  }
}

export function loginWithCode(code: string) {
  return requestJson<DashboardUser>("/api/auth/link-code", {
    method: "POST",
    body: JSON.stringify({ code }),
  });
}

export function currentUser() {
  return requestJson<DashboardUser>("/api/auth/me");
}

export async function logout() {
  await fetch("/api/auth/logout", {
    method: "POST",
    credentials: "include",
  });
}

export function getSummary() {
  return requestJson<DashboardSummary>("/api/dashboard/summary");
}

export function getExpenses(params: {
  month: string;
  category?: string;
  source?: string;
  page: number;
  size: number;
}) {
  const search = new URLSearchParams({
    month: params.month,
    page: String(params.page),
    size: String(params.size),
  });
  if (params.category) {
    search.set("category", params.category);
  }
  if (params.source) {
    search.set("source", params.source);
  }
  return requestJson<PageResponse<Expense>>(`/api/expenses?${search}`);
}

export function createExpense(form: ExpenseForm) {
  return requestJson<Expense>("/api/expenses", {
    method: "POST",
    body: JSON.stringify({
      amount: Number(form.amount),
      category: form.category,
      description: form.description,
      expenseDate: form.expenseDate,
      merchant: form.merchant || null,
    }),
  });
}

export function updateExpense(id: number, form: ExpenseForm) {
  return requestJson<Expense>(`/api/expenses/${id}`, {
    method: "PATCH",
    body: JSON.stringify({
      amount: Number(form.amount),
      category: form.category,
      description: form.description,
      expenseDate: form.expenseDate,
      merchant: form.merchant || null,
    }),
  });
}

export function deleteExpense(id: number) {
  return requestJson<Expense>(`/api/expenses/${id}`, {
    method: "DELETE",
  });
}

export function getRecurringExpenses() {
  return requestJson<RecurringExpense[]>("/api/recurring-expenses");
}

export function createRecurringExpense(form: RecurringForm) {
  return requestJson<RecurringExpense>("/api/recurring-expenses", {
    method: "POST",
    body: JSON.stringify({
      amount: Number(form.amount),
      category: form.category,
      description: form.description,
      merchant: form.merchant || null,
      frequency: form.frequency,
      nextRunDate: form.nextRunDate,
    }),
  });
}

export function deleteRecurringExpense(id: number) {
  return requestJson<RecurringExpense>(`/api/recurring-expenses/${id}`, {
    method: "DELETE",
  });
}
