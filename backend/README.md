# MeteoSA API

Custom REST API for the MeteoSA Android app. Node.js + Express, backed by a Supabase
(managed Postgres) database, deployed on Render.

## Endpoints

| Method | Path                      | Auth? | Description                                   |
|--------|---------------------------|-------|------------------------------------------------|
| POST   | `/api/auth/register`      | No    | Create an account. Body: `email`, `password`, `displayName`. |
| POST   | `/api/auth/login`         | No    | Log in. Body: `email`, `password`. Returns a JWT. |
| POST   | `/api/auth/google`        | No    | SSO (Part 3). Body: `idToken` (a Google ID token from Credential Manager). Verified against Google's public keys before a session is issued - see "SSO" below. |
| GET    | `/api/users/profile`      | Yes   | Returns the logged-in user's profile (incl. gamification points, avatar URL). |
| PUT    | `/api/users/fcm-token`    | Yes   | Push notifications (Part 3). Body: `token` - the device's current Firebase Cloud Messaging token. |
| PUT    | `/api/users/avatar`       | Yes   | Profile picture upload (Part 3). Body: `imageBase64`, `contentType` (`image/jpeg`\|`image/png`). Uploads to Supabase Storage and returns `avatarUrl`. |
| GET    | `/api/reports`            | Yes   | Lists recent community weather-impact reports. Optional `lat`, `lon`, `radiusKm` query params to filter by distance. |
| POST   | `/api/reports`            | Yes   | Submit a report. Body: `latitude`, `longitude`, `reportType` (`flood`\|`hail`\|`wind`\|`fire`\|`other`), `description`. Awards the user 10 points. Fire/flood reports also trigger a real-time push alert to other users (Part 3) - see "Push notifications" below. |
| DELETE | `/api/reports/:reportId`  | Yes   | Delete your own report (reverses its points). |

Authenticated requests send `Authorization: Bearer <jwt>`.

Passwords are hashed with **bcrypt** server-side before being stored (see
`src/routes/auth.js`) - the plaintext password is never written to the database or logs.
Accounts created via Google SSO have no password hash at all (`password_hash` is nullable).

## 1. Create the Supabase database

1. Go to [supabase.com](https://supabase.com), sign up (free), and create a new project.
2. Once it's provisioned, open **SQL Editor**, paste the contents of `src/db/schema.sql`, and
   click **Run**. This creates the `users` and `community_reports` tables.
3. Click **Connect** on your project dashboard, and under connection type pick **Transaction
   pooler** (not "Direct connection" - that host is IPv6-only and won't resolve on most
   home/mobile networks unless you pay for Supabase's IPv4 add-on). Copy that URI - you'll need
   it as `DATABASE_URL` below. Replace `[YOUR-PASSWORD]` in it with the database password you
   set when creating the project. It should look like:
   `postgresql://postgres.<project-ref>:<password>@aws-<n>-<region>.pooler.supabase.com:6543/postgres`

## 2. Run it locally

```bash
cd backend
npm install
cp .env.example .env
# edit .env: paste your Supabase DATABASE_URL, and set JWT_SECRET to a random string, e.g.
#   node -e "console.log(require('crypto').randomBytes(48).toString('hex'))"
npm start
```

You should see `MeteoSA API listening on port 3000`. Test it:

```bash
curl http://localhost:3000/
# -> {"status":"ok","service":"MeteoSA API"}
```

### Pointing the Android app at your local backend

The app on your phone can't reach `localhost` on your PC directly. With the phone connected
over USB, run:

```bash
adb reverse tcp:3000 tcp:3000
```

This forwards the phone's `127.0.0.1:3000` to your PC's `3000`. Then set, in the project's
`local.properties`:

```
BACKEND_BASE_URL=http://127.0.0.1:3000/
```

## 3. Deploy to Render

1. Push this repo to GitHub (see the root README).
2. On [render.com](https://render.com), **New -> Web Service**, connect your GitHub repo.
3. Root directory: `backend`. Build command: `npm install`. Start command: `npm start`.
4. Add environment variables under **Environment**: `DATABASE_URL` and `JWT_SECRET` (same
   values as your local `.env`).
5. Deploy. Render gives you a URL like `https://meteosa-api.onrender.com`.
6. In the Android project's `local.properties`, set:
   ```
   BACKEND_BASE_URL=https://meteosa-api.onrender.com/
   ```
   (keep the trailing slash).

Render's free tier spins the service down after inactivity, so the first request after a
while can take 30-60s to respond - expected, not a bug, when demoing.

## 4. Part 3 configuration (SSO, push notifications, blob storage)

These three features degrade gracefully if unconfigured (SSO verification fails cleanly with a
400, push sends silently no-op after one logged warning, avatar upload fails cleanly), so the
app still runs without them - but for the full feature set, add these to `.env` (see
`.env.example` for the exact keys and where to find each value):

- **SSO**: `GOOGLE_WEB_CLIENT_ID` - the Web application OAuth client id from
  `google-services.json` (`client[0].oauth_client[]` entry with `client_type: 3`). Used to
  verify Google ID tokens the Android app sends to `POST /api/auth/google`.
- **Push notifications**: `GOOGLE_SERVICE_ACCOUNT_KEY` (the entire downloaded Firebase service
  account JSON, as one line) and `FIREBASE_PROJECT_ID`. Firebase Console -> Project settings ->
  Service accounts -> Generate new private key. Used to mint an FCM HTTP v1 access token (see
  `src/push/sendPush.js`) - no `firebase-admin` dependency needed, since `google-auth-library`
  (already required for SSO) can mint that token from the same kind of credential.
- **Profile picture upload**: `SUPABASE_URL` and `SUPABASE_SERVICE_ROLE_KEY`. Supabase dashboard
  -> Storage -> New bucket (Public), then Project Settings -> API. The bucket name must match
  `AVATAR_BUCKET` in `src/storage/supabaseStorage.js`. The service-role key bypasses Row Level
  Security entirely - it must only ever live server-side, never in the Android app.

On Render, add the same variables under **Environment** alongside `DATABASE_URL`/`JWT_SECRET`.

## Tests

```bash
npm test
```

Runs Node's built-in test runner (`node --test`) over `src/util/geo.test.js`, which checks the
Haversine distance calculation used to filter community reports by radius.
