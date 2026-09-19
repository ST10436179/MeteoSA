# MeteoSA API

Custom REST API for the MeteoSA Android app. Node.js + Express, backed by a Supabase
(managed Postgres) database, deployed on Render.

## Endpoints

| Method | Path                  | Auth? | Description                                   |
|--------|-----------------------|-------|------------------------------------------------|
| POST   | `/api/auth/register`  | No    | Create an account. Body: `email`, `password`, `displayName`. |
| POST   | `/api/auth/login`     | No    | Log in. Body: `email`, `password`. Returns a JWT. |
| GET    | `/api/users/profile`  | Yes   | Returns the logged-in user's profile (incl. gamification points). |
| GET    | `/api/reports`        | Yes   | Lists recent community weather-impact reports. Optional `lat`, `lon`, `radiusKm` query params to filter by distance. |
| POST   | `/api/reports`        | Yes   | Submit a report. Body: `latitude`, `longitude`, `reportType` (`flood`\|`hail`\|`wind`\|`fire`\|`other`), `description`. Awards the user 10 points. |

Authenticated requests send `Authorization: Bearer <jwt>`.

Passwords are hashed with **bcrypt** server-side before being stored (see
`src/routes/auth.js`) - the plaintext password is never written to the database or logs.

## 1. Create the Supabase database

1. Go to [supabase.com](https://supabase.com), sign up (free), and create a new project.
2. Once it's provisioned, open **SQL Editor**, paste the contents of `src/db/schema.sql`, and
   click **Run**. This creates the `users` and `community_reports` tables.
3. Go to **Project Settings -> Database -> Connection string -> URI**. Copy it - you'll need it
   as `DATABASE_URL` below. (Supabase's example URI has `[YOUR-PASSWORD]` in it - replace that
   with the database password you set when creating the project.)

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

## Tests

```bash
npm test
```

Runs Node's built-in test runner (`node --test`) over `src/util/geo.test.js`, which checks the
Haversine distance calculation used to filter community reports by radius.
