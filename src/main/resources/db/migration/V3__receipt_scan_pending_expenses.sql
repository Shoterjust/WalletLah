ALTER TABLE expenses
  ADD COLUMN merchant VARCHAR(255),
  ADD COLUMN source VARCHAR(30) NOT NULL DEFAULT 'MANUAL',
  ADD COLUMN receipt_image_file_id VARCHAR(255);

CREATE TABLE pending_expenses (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  telegram_user_id BIGINT NOT NULL,
  merchant VARCHAR(255),
  amount NUMERIC(10, 2),
  category VARCHAR(50),
  expense_date DATE,
  raw_ocr_text TEXT,
  receipt_image_file_id VARCHAR(255),
  confidence NUMERIC(5, 2),
  status VARCHAR(30) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ
);

CREATE INDEX idx_pending_expenses_user_status_created
  ON pending_expenses(telegram_user_id, status, created_at DESC);
