package com.meteosa.app.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Since we build ViewModels by hand (constructor injection from AppContainer) rather than
 * through Hilt, this generic factory just calls the given lambda instead of needing a
 * dedicated Factory class per screen.
 */
class GenericViewModelFactory<VM : ViewModel>(private val creator: () -> VM) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = creator() as T
}
