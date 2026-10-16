// Top-level build file. Individual module build files declare their own plugin versions
// against these, so this file just registers the plugins without applying them at the root.
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    // KSP (not kapt) for Room's annotation processor - faster, and Room's DAO code generation
    // can't be avoided the way Hilt's DI generation was (see AppContainer.kt for why we dodge
    // annotation processors where we can; Room doesn't offer a reflection-only alternative).
    id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply false
}
