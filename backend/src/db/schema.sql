-- Run this once against your Supabase Postgres database (Supabase dashboard -> SQL Editor
-- -> paste this whole file -> Run). Safe to re-run: every statement is idempotent.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS users (
    user_id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email               TEXT UNIQUE NOT NULL,
    -- Nullable: accounts created via Google SSO (Part 3) have no password of their own.
    password_hash       TEXT,
    display_name        TEXT NOT NULL,
    preferred_language  TEXT NOT NULL DEFAULT 'en',
    points              INTEGER NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Idempotent migration for databases created before Part 3's Google SSO: loosen the existing
-- NOT NULL constraint instead of recreating the table.
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;

-- Push notifications (Part 3): the device's current Firebase Cloud Messaging token, so the
-- backend can push real-time alerts to it. Nullable - not every user has granted notification
-- permission or opened the app since this column was added.
ALTER TABLE users ADD COLUMN IF NOT EXISTS fcm_token TEXT;

-- Profile picture upload (Part 3 User Defined feature): public URL of the image in the
-- Supabase Storage "avatars" bucket. Nullable - most users never set one.
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_url TEXT;

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
