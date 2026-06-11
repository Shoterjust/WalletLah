"use client";

import { FormEvent, useEffect, useMemo, useState } from "react";
import {
  ApiError,
  createExpense,
  createRecurringExpense,
  currentUser,
  deleteExpense,
  deleteRecurringExpense,
  getExpenses,
  getRecurringExpenses,
  getSummary,
  loginWithCode,
  logout,
  updateExpense,
} from "@/lib/api";
import type {
  DashboardSummary,
  DashboardUser,
  Expense,
  ExpenseForm,
  PageResponse,
  RecurringExpense,
  RecurringForm,
} from "@/lib/types";

const categories = [
  "FOOD",
  "TRANSPORT",
  "GROCERIES",
  "SCHOOL",
  "SUBSCRIPTIONS",
  "SHOPPING",
  "ENTERTAINMENT",
  "HEALTH",
  "FAMILY",
  "OTHERS",
];

const sources = ["MANUAL", "RECEIPT_SCAN", "RECURRING"];
const frequencies = ["DAILY", "WEEKLY", "MONTHLY"];

const today = () => new Date().toISOString().slice(0, 10);
const currentMonth = () => new Date().toISOString().slice(0, 7);

const emptyExpenseForm = (): ExpenseForm => ({
  amount: "",
  category: "FOOD",
  description: "",
  expenseDate: today(),
  merchant: "",
});

const emptyRecurringForm = (): RecurringForm => ({
  amount: "",
  category: "SUBSCRIPTIONS",
  description: "",
  merchant: "",
  frequency: "MONTHLY",
  nextRunDate: today(),
});

export function WalletLahDashboard() {
  const [user, setUser] = useState<DashboardUser | null>(null);
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [expenses, setExpenses] = useState<PageResponse<Expense> | null>(null);
  const [recurring, setRecurring] = useState<RecurringExpense[]>([]);
  const [loginCode, setLoginCode] = useState("");
  const [expenseForm, setExpenseForm] = useState<ExpenseForm>(emptyExpenseForm);
  const [recurringForm, setRecurringForm] = useState<RecurringForm>(emptyRecurringForm);
  const [editingExpenseId, setEditingExpenseId] = useState<number | null>(null);
  const [editForm, setEditForm] = useState<ExpenseForm>(emptyExpenseForm);
  const [month, setMonth] = useState(currentMonth);
  const [categoryFilter, setCategoryFilter] = useState("");
  const [sourceFilter, setSourceFilter] = useState("");
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const topCategory = useMemo(() => summary?.categories[0], [summary]);

  useEffect(() => {
    void bootstrap();
  }, []);

  useEffect(() => {
    if (user) {
      void loadExpenses();
    }
  }, [user, month, categoryFilter, sourceFilter, page]);

  async function bootstrap() {
    setLoading(true);
    setError(null);
    try {
      const authenticated = await currentUser();
      setUser(authenticated);
      await loadAll();
    } catch (caught) {
      if (caught instanceof ApiError && caught.status === 401) {
        setUser(null);
      } else {
        setError(errorMessage(caught));
      }
    } finally {
      setLoading(false);
    }
  }

  async function loadAll() {
    const [summaryData, expenseData, recurringData] = await Promise.all([
      getSummary(),
      getExpenses({ month, category: categoryFilter, source: sourceFilter, page, size: 10 }),
      getRecurringExpenses(),
    ]);
    setSummary(summaryData);
    setExpenses(expenseData);
    setRecurring(recurringData);
  }

  async function loadExpenses() {
    try {
      const expenseData = await getExpenses({
        month,
        category: categoryFilter,
        source: sourceFilter,
        page,
        size: 10,
      });
      setExpenses(expenseData);
    } catch (caught) {
      setError(errorMessage(caught));
    }
  }

  async function refreshAfterMutation() {
    const [summaryData, expenseData, recurringData] = await Promise.all([
      getSummary(),
      getExpenses({ month, category: categoryFilter, source: sourceFilter, page, size: 10 }),
      getRecurringExpenses(),
    ]);
    setSummary(summaryData);
    setExpenses(expenseData);
    setRecurring(recurringData);
  }

  async function handleLogin(event: FormEvent) {
    event.preventDefault();
    setSaving(true);
    setError(null);
    try {
      const authenticated = await loginWithCode(loginCode);
      setUser(authenticated);
      setLoginCode("");
      await loadAll();
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setSaving(false);
    }
  }

  async function handleLogout() {
    await logout();
    setUser(null);
    setSummary(null);
    setExpenses(null);
    setRecurring([]);
  }

  async function handleCreateExpense(event: FormEvent) {
    event.preventDefault();
    setSaving(true);
    setError(null);
    try {
      await createExpense(expenseForm);
      setExpenseForm(emptyExpenseForm());
      await refreshAfterMutation();
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setSaving(false);
    }
  }

  function startEdit(expense: Expense) {
    setEditingExpenseId(expense.id);
    setEditForm({
      amount: String(expense.amount),
      category: expense.category,
      description: expense.description ?? "",
      expenseDate: expense.expenseDate,
      merchant: expense.merchant ?? "",
    });
  }

  async function handleUpdateExpense(event: FormEvent) {
    event.preventDefault();
    if (!editingExpenseId) {
      return;
    }
    setSaving(true);
    setError(null);
    try {
      await updateExpense(editingExpenseId, editForm);
      setEditingExpenseId(null);
      await refreshAfterMutation();
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setSaving(false);
    }
  }

  async function handleDeleteExpense(id: number) {
    setSaving(true);
    setError(null);
    try {
      await deleteExpense(id);
      await refreshAfterMutation();
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setSaving(false);
    }
  }

  async function handleCreateRecurring(event: FormEvent) {
    event.preventDefault();
    setSaving(true);
    setError(null);
    try {
      await createRecurringExpense(recurringForm);
      setRecurringForm(emptyRecurringForm());
      await refreshAfterMutation();
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setSaving(false);
    }
  }

  async function handleDeleteRecurring(id: number) {
    setSaving(true);
    setError(null);
    try {
      await deleteRecurringExpense(id);
      await refreshAfterMutation();
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return (
      <main className="app-shell">
        <section className="loading-panel">Loading WalletLah dashboard...</section>
      </main>
    );
  }

  if (!user) {
    return (
      <main className="auth-shell">
        <section className="auth-panel">
          <div>
            <p className="eyebrow">WalletLah Dashboard</p>
            <h1>Sign in with Telegram</h1>
            <p className="auth-copy">Send /dashboard_link to your WalletLah bot, then enter the 6-digit code.</p>
          </div>
          <form className="auth-form" onSubmit={handleLogin}>
            <label htmlFor="login-code">Dashboard code</label>
            <input
              id="login-code"
              inputMode="numeric"
              maxLength={6}
              pattern="[0-9]{6}"
              value={loginCode}
              onChange={(event) => setLoginCode(event.target.value)}
              placeholder="482913"
              required
            />
            <button type="submit" disabled={saving}>
              {saving ? "Signing in" : "Sign in"}
            </button>
          </form>
          {error ? <p className="error-text">{error}</p> : null}
        </section>
      </main>
    );
  }

  return (
    <main className="app-shell">
      <header className="topbar">
        <div>
          <p className="eyebrow">WalletLah</p>
          <h1>Dashboard</h1>
        </div>
        <div className="topbar-actions">
          <span>{user.displayName || `Telegram ${user.telegramUserId}`}</span>
          <button className="secondary-button" onClick={handleLogout} type="button">
            Sign out
          </button>
        </div>
      </header>

      {error ? (
        <section className="notice" role="alert">
          {error}
        </section>
      ) : null}

      <section className="summary-grid">
        <Metric label="Spent this month" value={money(summary?.totalSpent)} />
        <Metric label="Remaining" value={summary?.remainingBudget == null ? "No budget" : money(summary.remainingBudget)} />
        <Metric label="Safe daily spend" value={money(summary?.safeDailySpend)} />
        <Metric label="Budget used" value={`${summary?.budgetUsedPercentage ?? 0}%`} />
      </section>

      <section className="content-grid">
        <section className="panel span-two">
          <div className="panel-heading">
            <div>
              <h2>Expenses</h2>
              <p>
                {summary?.monthLabel ?? "This month"} average: {money(summary?.averageDailySpend)} per day
              </p>
            </div>
            <div className="filter-row">
              <input type="month" value={month} onChange={(event) => {
                setPage(0);
                setMonth(event.target.value);
              }} />
              <select value={categoryFilter} onChange={(event) => {
                setPage(0);
                setCategoryFilter(event.target.value);
              }}>
                <option value="">All categories</option>
                {categories.map((category) => (
                  <option key={category} value={category}>
                    {displayCategory(category)}
                  </option>
                ))}
              </select>
              <select value={sourceFilter} onChange={(event) => {
                setPage(0);
                setSourceFilter(event.target.value);
              }}>
                <option value="">All sources</option>
                {sources.map((source) => (
                  <option key={source} value={source}>
                    {sourceLabel(source)}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <form className="expense-form" onSubmit={handleCreateExpense}>
            <input
              aria-label="Amount"
              value={expenseForm.amount}
              onChange={(event) => setExpenseForm({ ...expenseForm, amount: event.target.value })}
              placeholder="Amount"
              type="number"
              min="0.01"
              step="0.01"
              required
            />
            <select
              aria-label="Category"
              value={expenseForm.category}
              onChange={(event) => setExpenseForm({ ...expenseForm, category: event.target.value })}
            >
              {categories.map((category) => (
                <option key={category} value={category}>
                  {displayCategory(category)}
                </option>
              ))}
            </select>
            <input
              aria-label="Description"
              value={expenseForm.description}
              onChange={(event) => setExpenseForm({ ...expenseForm, description: event.target.value })}
              placeholder="Description"
              required
            />
            <input
              aria-label="Date"
              value={expenseForm.expenseDate}
              onChange={(event) => setExpenseForm({ ...expenseForm, expenseDate: event.target.value })}
              type="date"
              required
            />
            <input
              aria-label="Merchant"
              value={expenseForm.merchant}
              onChange={(event) => setExpenseForm({ ...expenseForm, merchant: event.target.value })}
              placeholder="Merchant"
            />
            <button type="submit" disabled={saving}>
              Add
            </button>
          </form>

          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Description</th>
                  <th>Category</th>
                  <th>Source</th>
                  <th className="amount-cell">Amount</th>
                  <th className="actions-cell">Actions</th>
                </tr>
              </thead>
              <tbody>
                {expenses?.items.length ? (
                  expenses.items.map((expense) => (
                    <tr key={expense.id}>
                      <td>{expense.expenseDate}</td>
                      <td>
                        <strong>{expense.description || "No description"}</strong>
                        <span>{expense.merchant || `#${expense.id}`}</span>
                      </td>
                      <td>{expense.categoryDisplayName}</td>
                      <td>{sourceLabel(expense.source)}</td>
                      <td className="amount-cell">{money(expense.amount)}</td>
                      <td className="actions-cell">
                        <button className="ghost-button" type="button" onClick={() => startEdit(expense)}>
                          Edit
                        </button>
                        <button className="danger-button" type="button" onClick={() => void handleDeleteExpense(expense.id)}>
                          Delete
                        </button>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={6} className="empty-cell">
                      No expenses for this filter.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          <div className="pagination-row">
            <button type="button" disabled={page === 0} onClick={() => setPage((value) => Math.max(0, value - 1))}>
              Previous
            </button>
            <span>
              Page {(expenses?.page ?? page) + 1} of {Math.max(expenses?.totalPages ?? 1, 1)}
            </span>
            <button
              type="button"
              disabled={!expenses || expenses.page + 1 >= expenses.totalPages}
              onClick={() => setPage((value) => value + 1)}
            >
              Next
            </button>
          </div>
        </section>

        <section className="panel">
          <div className="panel-heading compact">
            <div>
              <h2>Categories</h2>
              <p>{topCategory ? `${topCategory.displayName} leads this month` : "No category spend yet"}</p>
            </div>
          </div>
          <div className="category-list">
            {summary?.categories.length ? (
              summary.categories.map((item) => (
                <div className="category-row" key={item.category}>
                  <div>
                    <strong>{item.displayName}</strong>
                    <span>{money(item.total)}</span>
                  </div>
                  <div className="bar-track">
                    <div className="bar-fill" style={{ width: `${Math.min(Number(item.percentage), 100)}%` }} />
                  </div>
                  <small>{item.percentage}%</small>
                </div>
              ))
            ) : (
              <p className="empty-text">No spending logged this month.</p>
            )}
          </div>
        </section>

        <section className="panel">
          <div className="panel-heading compact">
            <div>
              <h2>Recurring</h2>
              <p>{recurring.length} active rules</p>
            </div>
          </div>
          <form className="stack-form" onSubmit={handleCreateRecurring}>
            <div className="form-row">
              <input
                aria-label="Recurring amount"
                value={recurringForm.amount}
                onChange={(event) => setRecurringForm({ ...recurringForm, amount: event.target.value })}
                placeholder="Amount"
                type="number"
                min="0.01"
                step="0.01"
                required
              />
              <select
                aria-label="Recurring frequency"
                value={recurringForm.frequency}
                onChange={(event) => setRecurringForm({ ...recurringForm, frequency: event.target.value })}
              >
                {frequencies.map((frequency) => (
                  <option key={frequency} value={frequency}>
                    {titleCase(frequency)}
                  </option>
                ))}
              </select>
            </div>
            <select
              aria-label="Recurring category"
              value={recurringForm.category}
              onChange={(event) => setRecurringForm({ ...recurringForm, category: event.target.value })}
            >
              {categories.map((category) => (
                <option key={category} value={category}>
                  {displayCategory(category)}
                </option>
              ))}
            </select>
            <input
              aria-label="Recurring description"
              value={recurringForm.description}
              onChange={(event) => setRecurringForm({ ...recurringForm, description: event.target.value })}
              placeholder="Description"
              required
            />
            <input
              aria-label="Recurring merchant"
              value={recurringForm.merchant}
              onChange={(event) => setRecurringForm({ ...recurringForm, merchant: event.target.value })}
              placeholder="Merchant"
            />
            <input
              aria-label="Next run date"
              value={recurringForm.nextRunDate}
              onChange={(event) => setRecurringForm({ ...recurringForm, nextRunDate: event.target.value })}
              type="date"
              required
            />
            <button type="submit" disabled={saving}>
              Add recurring
            </button>
          </form>
          <div className="recurring-list">
            {recurring.length ? (
              recurring.map((item) => (
                <div className="recurring-row" key={item.id}>
                  <div>
                    <strong>{item.description}</strong>
                    <span>
                      {money(item.amount)} {item.categoryDisplayName} | {titleCase(item.frequency)} | next {item.nextRunDate}
                    </span>
                  </div>
                  <button className="danger-button" type="button" onClick={() => void handleDeleteRecurring(item.id)}>
                    Delete
                  </button>
                </div>
              ))
            ) : (
              <p className="empty-text">No active recurring expenses.</p>
            )}
          </div>
        </section>
      </section>

      {editingExpenseId ? (
        <div className="modal-backdrop" role="presentation">
          <section className="edit-dialog" role="dialog" aria-modal="true" aria-labelledby="edit-title">
            <div className="panel-heading compact">
              <div>
                <h2 id="edit-title">Edit Expense</h2>
                <p>Expense #{editingExpenseId}</p>
              </div>
              <button className="ghost-button" type="button" onClick={() => setEditingExpenseId(null)}>
                Close
              </button>
            </div>
            <form className="stack-form" onSubmit={handleUpdateExpense}>
              <input
                aria-label="Edit amount"
                value={editForm.amount}
                onChange={(event) => setEditForm({ ...editForm, amount: event.target.value })}
                type="number"
                min="0.01"
                step="0.01"
                required
              />
              <select
                aria-label="Edit category"
                value={editForm.category}
                onChange={(event) => setEditForm({ ...editForm, category: event.target.value })}
              >
                {categories.map((category) => (
                  <option key={category} value={category}>
                    {displayCategory(category)}
                  </option>
                ))}
              </select>
              <input
                aria-label="Edit description"
                value={editForm.description}
                onChange={(event) => setEditForm({ ...editForm, description: event.target.value })}
                required
              />
              <input
                aria-label="Edit date"
                value={editForm.expenseDate}
                onChange={(event) => setEditForm({ ...editForm, expenseDate: event.target.value })}
                type="date"
                required
              />
              <input
                aria-label="Edit merchant"
                value={editForm.merchant}
                onChange={(event) => setEditForm({ ...editForm, merchant: event.target.value })}
                placeholder="Merchant"
              />
              <button type="submit" disabled={saving}>
                Save changes
              </button>
            </form>
          </section>
        </div>
      ) : null}
    </main>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <section className="metric-card">
      <span>{label}</span>
      <strong>{value}</strong>
    </section>
  );
}

function money(value: number | undefined | null) {
  if (value == null) {
    return "S$0.00";
  }
  return `S$${Number(value).toFixed(2)}`;
}

function displayCategory(value: string) {
  return titleCase(value.replaceAll("_", " "));
}

function titleCase(value: string) {
  return value
    .toLowerCase()
    .split(" ")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

function sourceLabel(value: string) {
  if (value === "RECEIPT_SCAN") {
    return "Receipt";
  }
  return titleCase(value);
}

function errorMessage(caught: unknown) {
  if (caught instanceof Error) {
    return caught.message;
  }
  return "Something went wrong.";
}
