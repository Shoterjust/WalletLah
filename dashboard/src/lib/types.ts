export type DashboardUser = {
  userId: number;
  telegramUserId: number;
  displayName: string | null;
};

export type CategoryBreakdown = {
  category: string;
  displayName: string;
  total: number;
  percentage: number;
};

export type DashboardSummary = {
  monthLabel: string;
  totalSpent: number;
  monthlyBudget: number | null;
  remainingBudget: number | null;
  daysLeftInMonth: number;
  safeDailySpend: number;
  daysElapsedInMonth: number;
  averageDailySpend: number;
  budgetUsedPercentage: number;
  categories: CategoryBreakdown[];
};

export type Expense = {
  id: number;
  amount: number;
  category: string;
  categoryDisplayName: string;
  description: string | null;
  expenseDate: string;
  merchant: string | null;
  source: string;
  recurringExpenseId: number | null;
  createdAt: string;
};

export type RecurringExpense = {
  id: number;
  amount: number;
  category: string;
  categoryDisplayName: string;
  description: string;
  merchant: string | null;
  frequency: string;
  nextRunDate: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
};

export type PageResponse<T> = {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
};

export type ExpenseForm = {
  amount: string;
  category: string;
  description: string;
  expenseDate: string;
  merchant: string;
};

export type RecurringForm = {
  amount: string;
  category: string;
  description: string;
  merchant: string;
  frequency: string;
  nextRunDate: string;
};
