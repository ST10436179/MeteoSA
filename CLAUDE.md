# MeteoSA - context for Claude Code

Student PoE project (IIE OPSC6312, Open Source Coding Intermediate). Part 1 and 2 were marked 94%. This repo is the Final PoE (Part 3, 100 marks). Rubric lives in the module PDF (OPSC6312POE.pdf); key Part 3 items: runs on a real phone, SSO, offline sync, push, multi-language (2 SA languages), User Defined 4 and 5, Play Store prep, GitHub/README/CI, release notes, demo video, AI-use write-up, final commit tagged "final PoE".

## Stack
- Android: Kotlin + Jetpack Compose, MVVM + repositories, hand-rolled DI (`di/AppContainer.kt`), package `com.meteosa.app`, single `:app` module. Room, WorkManager, Retrofit, DataStore, Coil, osmdroid, Credential Manager (Google SSO), FCM.
- Backend: `backend/` Node/Express, Postgres + Supabase Storage (bucket `Avatar`), FCM v1 push, JWT + BCrypt. Secrets in git-ignored `backend/.env`.

## Status (Final PoE)
Done: offline sync, isiZulu + Afrikaans, Google SSO, push, Saved Locations (UD4), Profile Picture (UD5), README + release notes, signed release build with R8 (APK + AAB built, APK signature verified).
Also done: backend deployed to Render (https://meteosa.onrender.com/, free tier sleeps ~30-60s cold start); release SHA-1 added in Firebase so Google sign-in works in release; forecast switched to OpenWeather One Call 3.0 (the old 2.5/forecast now returns only 4 entries) with fallback to 2.5; release verified: launch, Google sign-in, weather.
Pending: verify on phone with R8 release: reports (submit/list), push, avatar upload, language switch, offline sync. Then Play Store listing screenshots, new demo video + link in README, AI-use write-up, fix README (stale 2.5 endpoint text, "deployed on Render" must stay true, google-services.json contradiction), commit + tag `final-poe` (user said NOT to commit yet).

## Dev workflow
- Built and tested via VS Code + physical phone (adb), NOT Android Studio. Phone must authorise USB debugging.
- Gradle needs JDK 21: `export JAVA_HOME="C:\Users\Admin\.jdks\jbr-21.0.11"`. Android Studio's bundled JDK is too new for Gradle 8.7 (fails with "25.0.3").
- Build output is redirected to `%TEMP%\meteosa-app-build` (project is under OneDrive, which locks build files). Outputs: `%TEMP%\meteosa-app-build\outputs\{apk,bundle}\release\`.
- Debug builds use `BACKEND_BASE_URL` (http://127.0.0.1:3000/ via `adb reverse tcp:3000 tcp:3000`, cleartext allowed in debug only). Release builds use `BACKEND_BASE_URL_RELEASE` (defaults to the Render HTTPS URL), because release blocks cleartext.
- Installing release over a debug build fails (different signing key); uninstall first (clears local app data).
- Secrets (never commit): `local.properties` (OPEN_WEATHER_API_KEY, BACKEND_BASE_URL, BACKEND_BASE_URL_RELEASE), `keystore.properties`, `backend/.env`. Release keystore: `C:\Users\Admin\.android-keys\meteosa-release.jks` (back it up; losing it blocks Play updates).
- Commands: `./gradlew testDebugUnitTest`, `./gradlew assembleDebug`, `./gradlew assembleRelease`, `./gradlew bundleRelease`. Backend: `cd backend && npm test`.
- CI: `.github/workflows/android-ci.yml` (unit tests + debug APK), `backend-ci.yml`.

## Known issues / ideas (from analysis)
- README contradicts itself on whether google-services.json is committed; demo link still "to be added".
- Backend: no rate limiting/helmet, open CORS, `JWT_SECRET` unvalidated, push goes to any 100 users, avatar content type unchecked.
- `SettingsScreen.kt` uses `session!!`; offline sync retries can double-submit; tests are thin (2 Android, 1 backend).

## Conventions
- Comment code and use logging (rubric asks for it). Keep README screenshots in `docs/screenshots/`.
- Only commit/push when the user asks.
