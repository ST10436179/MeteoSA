const jwt = require('jsonwebtoken');

/** Verifies the "Authorization: Bearer <jwt>" header and attaches req.userId. */
function requireAuth(req, res, next) {
    const header = req.headers.authorization || '';
    const token = header.startsWith('Bearer ') ? header.slice('Bearer '.length) : null;

    if (!token) {
        return res.status(401).json({ error: 'Missing or malformed Authorization header.' });
    }

    try {
        const payload = jwt.verify(token, process.env.JWT_SECRET);
        req.userId = payload.userId;
        next();
    } catch (err) {
        return res.status(401).json({ error: 'Invalid or expired session. Please log in again.' });
    }
}

module.exports = { requireAuth };
