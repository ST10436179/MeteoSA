package com.meteosa.app.util

/** Generic loading/success/error wrapper shared by every screen's ViewModel. */
sealed class UiState<out T> {
    data object Idle : UiState<Nothing>()
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

/** Turns a Retrofit/network exception into a message that's safe and useful to show a user. */
fun Throwable.toUserMessage(): String = when (this) {
    is java.net.UnknownHostException -> "No internet connection. Check your network and try again."
    is java.net.SocketTimeoutException -> "The server took too long to respond. Please try again."
    is retrofit2.HttpException -> when (code()) {
        401 -> "Incorrect email or password."
        409 -> "An account with that email already exists."
        404 -> "That resource could not be found."
        in 500..599 -> "The server ran into a problem. Please try again shortly."
        else -> "Something went wrong (code ${code()})."
    }
    else -> message ?: "An unexpected error occurred."
}
