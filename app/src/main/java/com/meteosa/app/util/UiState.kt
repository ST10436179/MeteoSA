package com.meteosa.app.util

/** Generic loading/success/error wrapper shared by every screen's ViewModel. */
sealed class UiState<out T> {
    data object Idle : UiState<Nothing>()
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

/**
 * Turns a Retrofit/network exception into a message that's safe and useful to show a user.
 *
 * Note: 401 is deliberately NOT mapped to "incorrect email or password" here, because this
 * function is shared by every screen, not just login - a 401 from the weather API (e.g. an
 * invalid/missing OpenWeatherMap key) or an expired session on any authenticated call would
 * otherwise show a misleading "wrong password" message on screens that have nothing to do with
 * login. AuthViewModel maps 401 to that specific wording itself, since it's the one call site
 * where a 401 genuinely does mean bad credentials.
 */
fun Throwable.toUserMessage(): String = when (this) {
    is java.net.UnknownHostException -> "No internet connection. Check your network and try again."
    is java.net.SocketTimeoutException -> "The server took too long to respond. Please try again."
    is retrofit2.HttpException -> when (code()) {
        401 -> "Not authorized (401). Check your session or API configuration."
        409 -> "An account with that email already exists."
        404 -> "That resource could not be found."
        in 500..599 -> "The server ran into a problem. Please try again shortly."
        else -> "Something went wrong (code ${code()})."
    }
    else -> message ?: "An unexpected error occurred."
}
