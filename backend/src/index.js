require('dotenv').config();
const express = require('express');
const cors = require('cors');

const { router: authRouter } = require('./routes/auth');
const usersRouter = require('./routes/users');
const reportsRouter = require('./routes/reports');

const app = express();
app.use(cors());
app.use(express.json());

// Simple request log - helps a lot when demoing the API in the Part 2 video.
app.use((req, res, next) => {
    console.log(`${new Date().toISOString()} ${req.method} ${req.path}`);
    next();
});

app.get('/', (req, res) => {
    res.json({ status: 'ok', service: 'MeteoSA API' });
});

app.use('/api/auth', authRouter);
app.use('/api/users', usersRouter);
app.use('/api/reports', reportsRouter);

app.use((req, res) => {
    res.status(404).json({ error: 'Not found.' });
});

// Catch-all error handler so a thrown error never leaks a raw stack trace to the client.
// eslint-disable-next-line no-unused-vars
app.use((err, req, res, next) => {
    console.error('Unhandled error:', err);
    res.status(500).json({ error: 'Internal server error.' });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`MeteoSA API listening on port ${PORT}`);
});
