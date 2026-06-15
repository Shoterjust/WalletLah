ALTER TABLE pending_expenses
  ADD COLUMN source VARCHAR(30) NOT NULL DEFAULT 'RECEIPT_SCAN',
  ADD COLUMN source_provider VARCHAR(50),
  ADD COLUMN source_message_id VARCHAR(500),
  ADD COLUMN source_sender VARCHAR(500),
  ADD COLUMN source_subject VARCHAR(1000);

ALTER TABLE email_expense_ingests
  ADD COLUMN source_provider VARCHAR(50);
