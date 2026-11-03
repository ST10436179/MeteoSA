const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { OAuth2Client } = require('google-auth-library');
const pool = require('../db/pool');

const router = express.Router();
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const googleClient = new OAuth2Client(process.env.GOOGLE_WEB_CLIENT_ID);

function toUserDto(row) {
    return {
        userId: row.user_id,
        email: row.email,
        displayName: row.display_name,
        preferredLanguage: row.preferred_language,
        points: row.points,
        avatarUrl: row.avatar_url || null
    };
}

function signToken(userId) {
    return jwt.sign({ userId }, process.env.JWT_SECRET, { expiresIn: '30d' });
}

// POST /api/auth/register
// The client sends the plaintext password over HTTPS (TLS protects it in transit, same as any
// login form on the web). We hash it here with bcrypt before it ever reaches the database, and
// the plaintext is never stored or logged - this is the standard place to do BCrypt hashing,
// since only the server can choose/verify the per-user salt bcrypt generates.
router.post('/register', async (req, res) => {
    const { email, password, displayName } = req.body || {};

    if (!email || !EMAIL_REGEX.test(email)) {
        return res.status(400).json({ error: 'A valid email is required.' });
    }
    if (!password || password.length < 6) {
        return res.status(400).json({ error: 'Password must be at least 6 characters.' });
    }
    if (!displayName || !displayName.trim()) {
        return res.status(400).json({ error: 'A display name is required.' });
    }

    try {
        const existing = await pool.query('SELECT 1 FROM users WHERE email = $1', [email.toLowerCase()]);
        if (existing.rowCount > 0) {
            return res.status(409).json({ error: 'An account with that email already exists.' });
        }

        const passwordHash = await bcrypt.hash(password, 10);
        const result = await pool.query(
            `INSERT INTO users (email, password_hash, display_name)
             VALUES ($1, $2, $3)
             RETURNING user_id, email, display_name, preferred_language, points`,
            [email.toLowerCase(), passwordHash, displayName.trim()]
        );

        const user = toUserDto(result.rows[0]);
        res.status(201).json({ token: signToken(user.userId), user });
    } catch (err) {
        console.error('Register failed:', err);
        res.status(500).json({ error: 'Could not create account. Please try again.' });
    }
});

// POST /api/auth/login
router.post('/login', async (req, res) => {
    const { email, password } = req.body || {};
    if (!email || !password) {
        return res.status(400).json({ error: 'Email and password are required.' });
    }

    try {
        const result = await pool.query('SELECT * FROM users WHERE email = $1', [email.toLowerCase()]);
        if (result.rowCount === 0) {
            return res.status(401).json({ error: 'Incorrect email or password.' });
        }

        const row = result.rows[0];
        const matches = await bcrypt.compare(password, row.password_hash);
        if (!matches) {
            return res.status(401).json({ error: 'Incorrect email or password.' });
        }

        const user = toUserDto(row);
        res.json({ token: signToken(user.userId), user });
    } catch (err) {
        console.error('Login failed:', err);
        res.status(500).json({ error: 'Could not log in. Please try again.' });
    }
});

// POST /api/auth/google
// SSO (Part 3): the Android app gets a Google ID token via Credential Manager and sends it
// here. We verify it against Google's own public keys (so it's cryptographically confirmed to
// have been issued by Google for this project's web client id, not something the client could
// forge), then find-or-create the user by the verified email and issue our usual session JWT -
// from this point on a Google-signed-in user is indistinguishable from an email/password one.
router.post('/google', async (req, res) => {
    const { idToken } = req.body || {};
    if (!idToken) {
        return res.status(400).json({ error: 'A Google ID token is required.' });
    }

    let payload;
    try {
        const ticket = await googleClient.verifyIdToken({
            idToken,
            audience: process.env.GOOGLE_WEB_CLIENT_ID
        });
        payload = ticket.getPayload();
    } catch (err) {
        console.error('Google ID token verification failed:', err.message);
        return res.status(401).json({ error: 'Could not verify Google sign-in. Please try again.' });
    }

    if (!payload?.email || !payload.email_verified) {
        return res.status(401).json({ error: 'Your Google account email could not be verified.' });
    }

    try {
        const email = payload.email.toLowerCase();
        const existing = await pool.query('SELECT * FROM users WHERE email = $1', [email]);

        let row;
        if (existing.rowCount > 0) {
            row = existing.rows[0];
        } else {
            const displayName = payload.name || email.split('@')[0];
            const inserted = await pool.query(
                `INSERT INTO users (email, password_hash, display_name)
                 VALUES ($1, NULL, $2)
                 RETURNING user_id, email, display_name, preferred_language, points`,
                [email, displayName]
            );
            row = inserted.rows[0];
        }

        const user = toUserDto(row);
        res.json({ token: signToken(user.userId), user });
    } catch (err) {
        console.error('Google sign-in failed:', err);
        res.status(500).json({ error: 'Could not sign in with Google. Please try again.' });
    }
});

module.exports = { router, toUserDto };
