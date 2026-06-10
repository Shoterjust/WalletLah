CREATE TABLE dashboard_link_codes (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES wallet_users(id) ON DELETE CASCADE,
  code_hash VARCHAR(64) NOT NULL,
  expires_at TIMESTAMPTZ NOT NULL,
  consumed_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_dashboard_link_codes_code_hash
  ON dashboard_link_codes(code_hash, consumed_at, expires_at);

CREATE INDEX idx_dashboard_link_codes_user_active
  ON dashboard_link_codes(user_id, consumed_at, expires_at);
