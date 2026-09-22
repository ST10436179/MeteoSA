package com.meteosa.app.ui.screens.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meteosa.app.data.repository.AuthRepository
import com.meteosa.app.util.UiState
import com.meteosa.app.util.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val uiState: StateFlow<UiState<Unit>> = _uiState.asStateFlow()

    /** Returns a validation error message, or null if the input is valid, WITHOUT calling the network. */
    fun validateLogin(email: String, password: String): String? = when {
        email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Enter a valid email address."
        password.isBlank() -> "Enter your password."
        else -> null
    }

    fun validateRegister(email: String, password: String, confirmPassword: String, displayName: String): String? = when {
        displayName.isBlank() -> "Enter a display name."
        email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Enter a valid email address."
        password.length < 6 -> "Password must be at least 6 characters."
        password != confirmPassword -> "Passwords do not match."
        else -> null
    }

    fun login(email: String, password: String) {
        val error = validateLogin(email, password)
        if (error != null) {
            _uiState.value = UiState.Error(error)
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
                    "Incorrect email or password."
                } else {
                    t.toUserMessage()
                }
                UiState.Error(message)
            }
        }
    }

    fun register(email: String, password: String, confirmPassword: String, displayName: String) {
        val error = validateRegister(email, password, confirmPassword, displayName)
        if (error != null) {
            _uiState.value = UiState.Error(error)
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

    fun resetState() {
        _uiState.value = UiState.Idle
    }
}
