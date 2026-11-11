-- QuizFin database schema (PostgreSQL 17)
-- Run once, e.g.:  "D:\PostgreSQL\bin\psql" -U postgres -f schema.sql
-- The app also creates the tables automatically on startup.

CREATE DATABASE quizfin;

\connect quizfin

-- ---------------------------------------------------------------- scores
CREATE TABLE IF NOT EXISTS scores (
    id         SERIAL PRIMARY KEY,
    player     VARCHAR(64) NOT NULL,
    score      INTEGER     NOT NULL,
    quiz_score INTEGER     NULL,          -- NULL = fish-only run
    fish_score INTEGER     NOT NULL,
    mode       VARCHAR(20) NOT NULL,      -- FISH_ONLY | QUIZ_AND_FISH
    played_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_scores_rank ON scores (score DESC, played_at ASC);

-- ----------------------------------------------------------------- users
-- One row per player account. The password column holds only a salted
-- PBKDF2-HMAC-SHA256 hash ("iterations$salt$hash"), never the password.
CREATE TABLE IF NOT EXISTS users (
    username      VARCHAR(20) PRIMARY KEY,      -- lower-case login name
    display_name  VARCHAR(64) NOT NULL DEFAULT '',
    password_hash TEXT        NOT NULL,
    created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login    TIMESTAMP   NULL              -- NULL until first login
);

CREATE INDEX IF NOT EXISTS idx_users_last_login ON users (last_login DESC);

-- Sanity checks after playing a game / signing in:
-- SELECT * FROM scores ORDER BY score DESC, played_at ASC LIMIT 10;
-- SELECT username, display_name, last_login FROM users ORDER BY created_at;
