package com.meteosa.app.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * The Web application OAuth client id from google-services.json (client_type 3), NOT the
 * Android client id - Credential Manager needs this one to issue a Google ID token our backend
 * can verify with audience checking. OAuth client ids are public identifiers (not secrets), so
 * it's fine for this to be a plain constant rather than something loaded from local.properties.
 */
private const val WEB_CLIENT_ID = "750559635923-jj66kc4bu8uq3fajfhhap84nqqgkfuh8.apps.googleusercontent.com"

/**
 * Launches the system Google account picker via Credential Manager (the modern replacement for
 * the deprecated GoogleSignInClient API) and returns the resulting Google ID token. [context]
 * should be an Activity context - Credential Manager needs one to show the picker UI.
 */
suspend fun requestGoogleIdToken(context: Context): String {
    val option = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(WEB_CLIENT_ID)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(option)
        .build()

    val credentialManager = CredentialManager.create(context)
    val result = credentialManager.getCredential(context, request)
    val googleCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
    return googleCredential.idToken
}
