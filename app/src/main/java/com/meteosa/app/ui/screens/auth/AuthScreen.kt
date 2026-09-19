package com.meteosa.app.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meteosa.app.data.repository.AuthRepository
import com.meteosa.app.util.GenericViewModelFactory
import com.meteosa.app.util.UiState

private enum class AuthTab { LOGIN, REGISTER }

@Composable
fun AuthScreen(
    authRepository: AuthRepository,
    onAuthenticated: () -> Unit
) {
    val viewModel: AuthViewModel = viewModel(
        factory = GenericViewModelFactory { AuthViewModel(authRepository) }
    )
    val uiState by viewModel.uiState.collectAsState()

    var tab by rememberSaveable { mutableStateOf(AuthTab.LOGIN) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var displayName by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    // LaunchedEffect (not a plain "if" in the composable body) so this side-effect - navigating
    // away and resetting state - runs exactly once when uiState transitions to Success, not on
    // every unrelated recomposition (e.g. a theme change) while it happens to still be Success.
    LaunchedEffect(uiState) {
        if (uiState is UiState.Success) {
            onAuthenticated()
            viewModel.resetState()
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.Cloud,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text("MeteoSA", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Hyper-local weather for South Africa",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))

            TabRow(selectedTabIndex = tab.ordinal, modifier = Modifier.fillMaxWidth()) {
                Tab(selected = tab == AuthTab.LOGIN, onClick = { tab = AuthTab.LOGIN; viewModel.resetState() }, text = { Text("Login") })
                Tab(selected = tab == AuthTab.REGISTER, onClick = { tab = AuthTab.REGISTER; viewModel.resetState() }, text = { Text("Register") })
            }
            Spacer(Modifier.height(24.dp))

            if (tab == AuthTab.REGISTER) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = passwordVisibility(passwordVisible),
                trailingIcon = { PasswordToggle(passwordVisible) { passwordVisible = !passwordVisible } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            if (tab == AuthTab.REGISTER) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm password") },
                    singleLine = true,
                    visualTransformation = passwordVisibility(passwordVisible),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(8.dp))
            if (uiState is UiState.Error) {
                Text(
                    (uiState as UiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (tab == AuthTab.LOGIN) {
                        viewModel.login(email, password)
                    } else {
                        viewModel.register(email, password, confirmPassword, displayName)
                    }
                },
                enabled = uiState !is UiState.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState is UiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(if (tab == AuthTab.LOGIN) "Login" else "Create account")
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { /* SSO ships in the final PoE release */ },
                enabled = false,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue with Google (final release)")
            }
        }
    }
}

private fun passwordVisibility(visible: Boolean): VisualTransformation =
    if (visible) VisualTransformation.None else PasswordVisualTransformation()

@Composable
private fun PasswordToggle(visible: Boolean, onToggle: () -> Unit) {
    val icon: ImageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
    IconButton(onClick = onToggle) {
        Icon(icon, contentDescription = if (visible) "Hide password" else "Show password")
    }
}
