ALTER TABLE expenses
  ADD COLUMN recurring_expense_id BIGINT;

CREATE TABLE recurring_expenses (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES wallet_users(id) ON DELETE CASCADE,
  amount NUMERIC(10, 2) NOT NULL CHECK (amount > 0),
  category VARCHAR(50) NOT NULL,
  description VARCHAR(255) NOT NULL,
  merchant VARCHAR(255),
  frequency VARCHAR(30) NOT NULL,
  next_run_date DATE NOT NULL,
  active BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE expenses
  ADD CONSTRAINT fk_expenses_recurring_expense
  FOREIGN KEY (recurring_expense_id)
  REFERENCES recurring_expenses(id)
  ON DELETE SET NULL;

CREATE INDEX idx_recurring_expenses_due
  ON recurring_expenses(active, next_run_date);

CREATE INDEX idx_recurring_expenses_user_active
  ON recurring_expenses(user_id, active, next_run_date);
