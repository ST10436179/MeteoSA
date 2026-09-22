const express = require('express');
const pool = require('../db/pool');
const { requireAuth } = require('../middleware/auth');
const { haversineKm } = require('../util/geo');

const router = express.Router();

const ALLOWED_TYPES = new Set(['flood', 'hail', 'wind', 'fire', 'other']);
const POINTS_PER_REPORT = 10;

function toReportDto(row) {
    return {
        reportId: row.report_id,
        userId: row.user_id,
        displayName: row.display_name,
        latitude: row.latitude,
        longitude: row.longitude,
        reportType: row.report_type,
        description: row.description,
        createdAt: row.created_at
    };
}

// GET /api/reports?lat=&lon=&radiusKm=
// Public (no auth required) so the community feed is viewable before logging in isn't needed
// here, but we still require auth app-wide via the interceptor on the Android side for
// consistency with the rest of the API. Returns the most recent 200 reports, optionally
// filtered to those within radiusKm of (lat, lon).
router.get('/', requireAuth, async (req, res) => {
    try {
        const result = await pool.query(
            `SELECT r.report_id, r.user_id, r.latitude, r.longitude, r.report_type, r.description, r.created_at,
                    u.display_name
             FROM community_reports r
             JOIN users u ON u.user_id = r.user_id
             ORDER BY r.created_at DESC
             LIMIT 200`
        );

        let reports = result.rows.map(toReportDto);

        const lat = parseFloat(req.query.lat);
        const lon = parseFloat(req.query.lon);
        const radiusKm = parseFloat(req.query.radiusKm);
        if (!Number.isNaN(lat) && !Number.isNaN(lon) && !Number.isNaN(radiusKm)) {
            reports = reports.filter((r) => haversineKm(lat, lon, r.latitude, r.longitude) <= radiusKm);
        }

        res.json(reports);
    } catch (err) {
        console.error('Fetch reports failed:', err);
        res.status(500).json({ error: 'Could not load community reports.' });
    }
});

// POST /api/reports
router.post('/', requireAuth, async (req, res) => {
    const { latitude, longitude, reportType, description } = req.body || {};

    if (typeof latitude !== 'number' || typeof longitude !== 'number') {
        return res.status(400).json({ error: 'latitude and longitude must be numbers.' });
    }
    if (!ALLOWED_TYPES.has(reportType)) {
        return res.status(400).json({ error: `reportType must be one of: ${[...ALLOWED_TYPES].join(', ')}.` });
    }
    if (!description || !description.trim()) {
        return res.status(400).json({ error: 'A description is required.' });
    }

    const client = await pool.connect();
    try {
        await client.query('BEGIN');

        const inserted = await client.query(
            `INSERT INTO community_reports (user_id, latitude, longitude, report_type, description)
             VALUES ($1, $2, $3, $4, $5)
             RETURNING report_id, user_id, latitude, longitude, report_type, description, created_at`,
            [req.userId, latitude, longitude, reportType, description.trim()]
        );

        const pointsResult = await client.query(
            'UPDATE users SET points = points + $1 WHERE user_id = $2 RETURNING points, display_name',
            [POINTS_PER_REPORT, req.userId]
        );

        await client.query('COMMIT');

        const reportRow = { ...inserted.rows[0], display_name: pointsResult.rows[0].display_name };
        res.status(201).json({
            report: toReportDto(reportRow),
            points: pointsResult.rows[0].points
        });
    } catch (err) {
        await client.query('ROLLBACK');
        console.error('Submit report failed:', err);
        res.status(500).json({ error: 'Could not submit report.' });
    } finally {
        client.release();
    }
});

// DELETE /api/reports/:reportId - only the report's author may delete their own report. Also
// reverses the points that report earned, so deleting a mistaken report doesn't leave the user
// with points for something that no longer exists.
router.delete('/:reportId', requireAuth, async (req, res) => {
    const { reportId } = req.params;
    const client = await pool.connect();
    try {
        await client.query('BEGIN');

        const existing = await client.query(
            'SELECT user_id FROM community_reports WHERE report_id = $1',
            [reportId]
        );
        if (existing.rows.length === 0) {
            await client.query('ROLLBACK');
            return res.status(404).json({ error: 'Report not found.' });
        }
        if (existing.rows[0].user_id !== req.userId) {
            await client.query('ROLLBACK');
            return res.status(403).json({ error: 'You can only delete your own reports.' });
        }

        await client.query('DELETE FROM community_reports WHERE report_id = $1', [reportId]);

        const pointsResult = await client.query(
            'UPDATE users SET points = GREATEST(points - $1, 0) WHERE user_id = $2 RETURNING points',
            [POINTS_PER_REPORT, req.userId]
        );

        await client.query('COMMIT');
        res.json({ points: pointsResult.rows[0].points });
    } catch (err) {
        await client.query('ROLLBACK');
        console.error('Delete report failed:', err);
        res.status(500).json({ error: 'Could not delete report.' });
    } finally {
        client.release();
    }
});

module.exports = router;
