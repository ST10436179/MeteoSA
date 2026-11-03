const { createClient } = require('@supabase/supabase-js');

const AVATAR_BUCKET = 'Avatar';

let cachedClient = null;

function getClient() {
    if (!process.env.SUPABASE_URL || !process.env.SUPABASE_SERVICE_ROLE_KEY) {
        throw new Error('SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY must be set to upload avatars.');
    }
    if (!cachedClient) {
        // The service role key bypasses Row Level Security, which is fine here - this client
        // only ever runs server-side, never shipped to the Android app.
        cachedClient = createClient(process.env.SUPABASE_URL, process.env.SUPABASE_SERVICE_ROLE_KEY);
    }
    return cachedClient;
}

/**
 * Uploads a profile picture for [userId] (overwriting any previous one at the same path) and
 * returns its public URL. [buffer] is the raw image bytes, [contentType] e.g. "image/jpeg".
 */
async function uploadAvatar(userId, buffer, contentType) {
    const client = getClient();
    const extension = contentType === 'image/png' ? 'png' : 'jpg';
    const path = `${userId}.${extension}`;

    const { error: uploadError } = await client.storage
        .from(AVATAR_BUCKET)
        .upload(path, buffer, { contentType, upsert: true });

    if (uploadError) {
        throw uploadError;
    }

    const { data } = client.storage.from(AVATAR_BUCKET).getPublicUrl(path);
    // Cache-bust so the app's image loader picks up the new picture immediately instead of
    // reusing a cached copy at the same URL.
    return `${data.publicUrl}?t=${Date.now()}`;
}

module.exports = { uploadAvatar };
