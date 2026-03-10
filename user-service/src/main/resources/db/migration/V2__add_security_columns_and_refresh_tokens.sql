-- Add security columns to users table
ALTER TABLE users.users ADD COLUMN IF NOT EXISTS is_enabled BOOLEAN DEFAULT TRUE;
ALTER TABLE users.users ADD COLUMN IF NOT EXISTS is_locked BOOLEAN DEFAULT FALSE;

-- Create refresh_tokens table for token revocation
CREATE TABLE users.refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token VARCHAR(500) NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES users.users(id) ON DELETE CASCADE,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_token ON users.refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_user_id ON users.refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON users.refresh_tokens(expires_at);
