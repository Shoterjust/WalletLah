ALTER TABLE wallet_users
  ADD COLUMN email_address TEXT;

CREATE UNIQUE INDEX uk_wallet_users_email_address_lower
  ON wallet_users (lower(email_address))
  WHERE email_address IS NOT NULL;

CREATE TABLE email_expense_ingests (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES wallet_users(id) ON DELETE CASCADE,
  expense_id BIGINT REFERENCES expenses(id) ON DELETE SET NULL,
  source_message_id TEXT,
  sender TEXT,
  subject TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uk_email_expense_ingests_user_message
  ON email_expense_ingests(user_id, source_message_id)
  WHERE source_message_id IS NOT NULL;
