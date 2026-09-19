-- Run this once against your Supabase Postgres database (Supabase dashboard -> SQL Editor
-- -> paste this whole file -> Run). Safe to re-run: every statement is idempotent.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS users (
    user_id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email               TEXT UNIQUE NOT NULL,
    password_hash       TEXT NOT NULL,
    display_name        TEXT NOT NULL,
    preferred_language  TEXT NOT NULL DEFAULT 'en',
    points              INTEGER NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS community_reports (
    report_id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    latitude      DOUBLE PRECISION NOT NULL,
    longitude     DOUBLE PRECISION NOT NULL,
    report_type   TEXT NOT NULL,
    description   TEXT NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_community_reports_created_at ON community_reports (created_at DESC);
