CREATE TABLE IF NOT EXISTS users (id text PRIMARY KEY,email text UNIQUE NOT NULL,password_hash text,verified boolean NOT NULL DEFAULT false,pro boolean NOT NULL DEFAULT false,created_at timestamptz NOT NULL DEFAULT now());
CREATE TABLE IF NOT EXISTS trials (user_id text PRIMARY KEY,started_ms bigint NOT NULL);
CREATE TABLE IF NOT EXISTS sessions (token_hash text PRIMARY KEY,user_id text NOT NULL REFERENCES users(id) ON DELETE CASCADE,expires_at timestamptz NOT NULL);
CREATE TABLE IF NOT EXISTS email_tokens (token_hash text PRIMARY KEY,user_id text NOT NULL REFERENCES users(id) ON DELETE CASCADE,kind text NOT NULL,expires_at timestamptz NOT NULL);
CREATE TABLE IF NOT EXISTS backups (user_id text PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,revision integer NOT NULL,data text NOT NULL,updated_at timestamptz NOT NULL DEFAULT now());
CREATE TABLE IF NOT EXISTS purchases (token_hash text PRIMARY KEY,user_id text NOT NULL,token_encrypted text NOT NULL,updated_at timestamptz NOT NULL DEFAULT now());
CREATE INDEX IF NOT EXISTS purchases_owner ON purchases(user_id);
CREATE TABLE IF NOT EXISTS auth_challenges (nonce_hash text PRIMARY KEY,expires_at timestamptz NOT NULL);
