const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const pool = require('../db/pool');

const router = express.Router();
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function toUserDto(row) {
    return {
        userId: row.user_id,
        email: row.email,
        displayName: row.display_name,
        preferredLanguage: row.preferred_language,
        points: row.points
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

module.exports = { router, toUserDto };
