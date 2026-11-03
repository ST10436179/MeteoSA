package com.meteosa.app.ui.screens.auth

import android.content.Context
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meteosa.app.R
import com.meteosa.app.data.repository.AuthRepository
import com.meteosa.app.util.UiState
import com.meteosa.app.util.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * [appContext] is the application Context (safe to hold - it outlives the ViewModel, unlike an
 * Activity Context) used only to resolve localised validation-error strings via getString().
 */
class AuthViewModel(
    private val authRepository: AuthRepository,
    private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val uiState: StateFlow<UiState<Unit>> = _uiState.asStateFlow()

    /** Returns a validation error resource id, or null if the input is valid, WITHOUT calling the network. */
    fun validateLoginRes(email: String, password: String): Int? = when {
        email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> R.string.error_invalid_email
        password.isBlank() -> R.string.error_empty_password
        else -> null
    }

    fun validateRegisterRes(email: String, password: String, confirmPassword: String, displayName: String): Int? = when {
        displayName.isBlank() -> R.string.error_empty_display_name
        email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> R.string.error_invalid_email
        password.length < 6 -> R.string.error_password_too_short
        password != confirmPassword -> R.string.error_passwords_mismatch
        else -> null
    }

    fun login(email: String, password: String) {
        val errorRes = validateLoginRes(email, password)
        if (errorRes != null) {
            _uiState.value = UiState.Error(appContext.getString(errorRes))
            return
        }
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                authRepository.login(email.trim(), password)
                UiState.Success(Unit)
            } catch (t: Throwable) {
                // A 401 specifically on /api/auth/login always means bad credentials - that's
                // the one call site where that wording is actually correct (see UiState.kt for
                // why the shared toUserMessage() doesn't assume that generally).
                val message = if (t is retrofit2.HttpException && t.code() == 401) {
                    appContext.getString(R.string.error_incorrect_credentials)
                } else {
                    t.toUserMessage()
                }
                UiState.Error(message)
            }
        }
    }

    fun register(email: String, password: String, confirmPassword: String, displayName: String) {
        val errorRes = validateRegisterRes(email, password, confirmPassword, displayName)
        if (errorRes != null) {
            _uiState.value = UiState.Error(appContext.getString(errorRes))
            return
        }
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                authRepository.register(email.trim(), password, displayName.trim())
                UiState.Success(Unit)
            } catch (t: Throwable) {
                UiState.Error(t.toUserMessage())
            }
        }
    }

    /** SSO (Part 3): [idToken] has already been obtained from Credential Manager by the caller
     *  (AuthScreen), since fetching it requires an Activity context this ViewModel doesn't have. */
    fun loginWithGoogle(idToken: String) {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                authRepository.loginWithGoogle(idToken)
                UiState.Success(Unit)
            } catch (t: Throwable) {
                UiState.Error(t.toUserMessage())
            }
        }
    }

    fun setError(message: String) {
        _uiState.value = UiState.Error(message)
    }

    fun resetState() {
        _uiState.value = UiState.Idle
    }
}
