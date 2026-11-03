const express = require('express');
const pool = require('../db/pool');
const { requireAuth } = require('../middleware/auth');
const { toUserDto } = require('./auth');

const router = express.Router();

// GET /api/users/profile
router.get('/profile', requireAuth, async (req, res) => {
    try {
        const result = await pool.query(
            'SELECT user_id, email, display_name, preferred_language, points FROM users WHERE user_id = $1',
            [req.userId]
        );
        if (result.rowCount === 0) {
            return res.status(404).json({ error: 'User not found.' });
        }
        res.json(toUserDto(result.rows[0]));
    } catch (err) {
        console.error('Fetch profile failed:', err);
        res.status(500).json({ error: 'Could not load profile.' });
    }
});

// PUT /api/users/fcm-token
// Push notifications (Part 3): called whenever the app obtains a fresh FCM token (first launch,
// or FirebaseMessagingService.onNewToken firing after a token rotation).
router.put('/fcm-token', requireAuth, async (req, res) => {
    const { token } = req.body || {};
    if (!token) {
        return res.status(400).json({ error: 'A token is required.' });
    }
    try {
        await pool.query('UPDATE users SET fcm_token = $1 WHERE user_id = $2', [token, req.userId]);
        res.status(204).send();
    } catch (err) {
        console.error('Save FCM token failed:', err);
        res.status(500).json({ error: 'Could not save push notification token.' });
    }
});

module.exports = router;
