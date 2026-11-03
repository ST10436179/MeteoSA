import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
}

// Read secrets/config out of local.properties (never committed) so they end up in
// BuildConfig instead of hardcoded in source.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(FileInputStream(f))
}
fun localProp(key: String, default: String) = (localProps.getProperty(key) ?: default)

// This project lives under a OneDrive-synced folder (Videos). OneDrive's file watcher
// intermittently grabs a lock on freshly-written build outputs (observed repeatedly with
// processDebugResources' R.jar), causing sporadic "couldn't delete" build failures that have
// nothing to do with the code. Redirecting build output to local temp - outside OneDrive's
// sync scope - avoids the conflict entirely. Harmless to delete/regenerate at any time.
layout.buildDirectory.set(File(System.getenv("TEMP") ?: System.getProperty("java.io.tmpdir"), "meteosa-app-build"))

android {
    namespace = "com.meteosa.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.meteosa.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0-prototype"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "OPEN_WEATHER_API_KEY",
            "\"${localProp("OPEN_WEATHER_API_KEY", "")}\""
        )
        buildConfigField(
            "String",
            "BACKEND_BASE_URL",
            "\"${localProp("BACKEND_BASE_URL", "http://127.0.0.1:3000/")}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        // Material3 components we use (TopAppBar, ExposedDropdownMenuBox, etc.) are still
        // marked @ExperimentalMaterial3Api upstream; this opts the whole module in rather than
        // annotating every composable that touches one of them individually.
        freeCompilerArgs += listOf("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Navigation + ViewModel
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    // .await() extension for Google Play Services / Firebase Task<T> (FCM token fetch, etc.)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // Secure local storage for the JWT session token
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Theme + settings persistence
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Device location for "weather near me"
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Community Impact map (Part 1 design doc): OpenStreetMap-based, no API key/account needed,
    // unlike Mapbox/Google Maps which both require a signed-up access token we don't have here.
    implementation("org.osmdroid:osmdroid-android:6.1.20")

    // Offline mode with sync (Part 3): RoomDB for local caching + a queue of not-yet-synced
    // community reports, WorkManager for the background sync job. Matches the sequence diagram
    // from the Part 1 design document exactly.
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // SSO (Part 3): Credential Manager is the modern replacement for the deprecated
    // GoogleSignInClient API, backed by the Google ID provider library below. We send the raw
    // Google ID token to our own backend for verification, rather than routing through Firebase
    // Authentication, so no separate service-account credential is needed server-side.
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Push notifications (Part 3): Firebase Cloud Messaging.
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-messaging-ktx")

    // Profile picture upload (Part 3 User Defined feature): loads the avatar URL returned by
    // the backend (Supabase Storage) into a Compose Image.
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Unit testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
