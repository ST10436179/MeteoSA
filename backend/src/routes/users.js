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

module.exports = router;
