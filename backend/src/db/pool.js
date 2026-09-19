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

module.exports = pool;
