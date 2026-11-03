const { GoogleAuth } = require('google-auth-library');

let cachedAuth = null;
let warnedMissingConfig = false;

function getAuth() {
    if (!process.env.GOOGLE_SERVICE_ACCOUNT_KEY) {
        if (!warnedMissingConfig) {
            console.warn('GOOGLE_SERVICE_ACCOUNT_KEY is not set - push notifications are disabled.');
            warnedMissingConfig = true;
        }
        return null;
    }
    if (!cachedAuth) {
        const credentials = JSON.parse(process.env.GOOGLE_SERVICE_ACCOUNT_KEY);
        cachedAuth = new GoogleAuth({
            credentials,
            scopes: ['https://www.googleapis.com/auth/firebase.messaging']
        });
    }
    return cachedAuth;
}

/**
 * Sends a push notification to a single FCM device token via Firebase's HTTP v1 API. Silently
 * no-ops (after one warning) if GOOGLE_SERVICE_ACCOUNT_KEY isn't configured, so report
 * submission never fails just because push notifications aren't set up in this environment -
 * see backend/.env.example for how to obtain and set that key.
 */
async function sendPushToToken(token, title, body) {
    const auth = getAuth();
    if (!auth || !token) return;

    try {
        const client = await auth.getClient();
        const accessTokenResponse = await client.getAccessToken();
        const accessToken = typeof accessTokenResponse === 'string'
            ? accessTokenResponse
            : accessTokenResponse.token;
        const projectId = process.env.FIREBASE_PROJECT_ID || 'meteosa';

        const response = await fetch(
            `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`,
            {
                method: 'POST',
                headers: {
                    Authorization: `Bearer ${accessToken}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    message: {
                        token,
                        notification: { title, body }
                    }
                })
            }
        );
        if (!response.ok) {
            const text = await response.text();
            console.error('FCM send failed:', response.status, text);
        }
    } catch (err) {
        console.error('FCM send error:', err.message);
    }
}

module.exports = { sendPushToToken };
