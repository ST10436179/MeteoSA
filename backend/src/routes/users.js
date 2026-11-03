const express = require('express');
const pool = require('../db/pool');
const { requireAuth } = require('../middleware/auth');
const { toUserDto } = require('./auth');
const { uploadAvatar } = require('../storage/supabaseStorage');

const router = express.Router();

// 2MB decoded - comfortably more than a downscaled profile picture needs, and small enough
// that a malicious/buggy client can't use this to store arbitrarily large blobs.
const MAX_AVATAR_BYTES = 2 * 1024 * 1024;
const ALLOWED_AVATAR_TYPES = new Set(['image/jpeg', 'image/png']);

// GET /api/users/profile
router.get('/profile', requireAuth, async (req, res) => {
    try {
        const result = await pool.query(
            'SELECT user_id, email, display_name, preferred_language, points, avatar_url FROM users WHERE user_id = $1',
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

// PUT /api/users/avatar
// Profile picture upload (Part 3 User Defined feature): the client sends the image as base64
// JSON rather than multipart, since it's small (downscaled/compressed client-side first) and
// this avoids adding a multipart-parsing dependency for a single endpoint.
router.put('/avatar', requireAuth, async (req, res) => {
    const { imageBase64, contentType } = req.body || {};

    if (!imageBase64 || !contentType) {
        return res.status(400).json({ error: 'imageBase64 and contentType are required.' });
    }
    if (!ALLOWED_AVATAR_TYPES.has(contentType)) {
        return res.status(400).json({ error: 'contentType must be image/jpeg or image/png.' });
    }

    let buffer;
    try {
        buffer = Buffer.from(imageBase64, 'base64');
    } catch (err) {
        return res.status(400).json({ error: 'imageBase64 is not valid base64.' });
    }
    if (buffer.length === 0 || buffer.length > MAX_AVATAR_BYTES) {
        return res.status(400).json({ error: 'Image must be between 1 byte and 2MB.' });
    }

    try {
        const avatarUrl = await uploadAvatar(req.userId, buffer, contentType);
        await pool.query('UPDATE users SET avatar_url = $1 WHERE user_id = $2', [avatarUrl, req.userId]);
        res.json({ avatarUrl });
    } catch (err) {
        console.error('Avatar upload failed:', err);
        res.status(500).json({ error: 'Could not upload profile picture.' });
    }
});

module.exports = router;
