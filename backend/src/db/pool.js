const { Pool } = require('pg');

if (!process.env.DATABASE_URL) {
    throw new Error('DATABASE_URL is not set. Copy backend/.env.example to backend/.env and fill it in.');
}

// Supabase (and Render) require TLS but commonly present a certificate chain that Node's
// default strict verification rejects in this kind of small-project setup, so we disable
// certificate verification for this connection specifically. The connection itself is still
// encrypted - only certificate authority validation is relaxed.
const pool = new Pool({
    connectionString: process.env.DATABASE_URL,
    ssl: { rejectUnauthorized: false }
});

// Without this, an idle pooled client being closed by Supabase's side (e.g. "terminating
// connection due to administrator command") fires an unhandled 'error' event on the pool,
// which Node treats as an uncaught exception and crashes the whole process.
pool.on('error', (err) => {
    console.error('Unexpected error on idle database client:', err.message);
});

module.exports = pool;
