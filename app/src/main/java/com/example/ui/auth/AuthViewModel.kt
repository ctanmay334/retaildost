package com.example.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.SessionManager
import com.example.domain.auth.ForgotPasswordUseCase
import com.example.domain.auth.LoginUseCase
import com.example.domain.auth.LogoutUseCase
import com.example.domain.auth.SignupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── UI State models ────────────────────────────────────────────────────────────

/** Unified auth UI state for all auth screens. */
data class AuthUiState(
    val isLoading:          Boolean = false,
    val isAuthenticated:    Boolean = false,
    val errorMessage:       String? = null,
    val successMessage:     String? = null,
    val isNewSignup:        Boolean = false,

    // Login form fields
    val loginEmail:         String  = "",
    val loginPassword:      String  = "",
    val loginPasswordVisible: Boolean = false,

    // Signup form fields
    val signupEmail:          String  = "",
    val signupPassword:       String  = "",
    val signupConfirmPassword: String  = "",
    val signupPasswordVisible: Boolean = false,
    val signupConfirmVisible:  Boolean = false,

    // Forgot password field
    val resetEmail:           String  = "",
    val resetEmailSent:       Boolean = false,

    // Validation errors per field
    val loginEmailError:         String? = null,
    val loginPasswordError:      String? = null,
    val signupEmailError:        String? = null,
    val signupPasswordError:     String? = null,
    val signupConfirmError:      String? = null,
    val resetEmailError:         String? = null
)

// ── AuthViewModel ──────────────────────────────────────────────────────────────

/**
 * AuthViewModel
 * ─────────────
 * Hilt-injected ViewModel responsible for:
 *  • Cold-start session restoration (auto-login if tokens are valid)
 *  • Login / Signup / Logout / ForgotPassword flows
 *  • Exposing [AuthUiState] to Compose screens via [StateFlow]
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase:         LoginUseCase,
    private val signupUseCase:        SignupUseCase,
    private val logoutUseCase:        LogoutUseCase,
    private val forgotPasswordUseCase: ForgotPasswordUseCase,
    private val sessionManager:       SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // Session loading guard — prevents navigation flicker on startup
    private val _isSessionLoading = MutableStateFlow(true)
    val isSessionLoading: StateFlow<Boolean> = _isSessionLoading.asStateFlow()

    init {
        restoreSession()
    }

    // ── Session restoration ───────────────────────────────────────────────────

    /**
     * Tries to restore a saved session from [EncryptedSharedPreferences].
     * Sets [isAuthenticated] = true if a valid session is found.
     * Always clears [_isSessionLoading] when done.
     */
    private fun restoreSession() {
        viewModelScope.launch {
            val restored = sessionManager.restoreSession()
            _uiState.update { it.copy(isAuthenticated = restored, isNewSignup = false) }
            _isSessionLoading.value = false
        }
    }

    // ── Form field updates ────────────────────────────────────────────────────

    fun onLoginEmailChanged(value: String) =
        _uiState.update { it.copy(loginEmail = value, loginEmailError = null, errorMessage = null) }

    fun onLoginPasswordChanged(value: String) =
        _uiState.update { it.copy(loginPassword = value, loginPasswordError = null, errorMessage = null) }

    fun onLoginPasswordVisibilityToggle() =
        _uiState.update { it.copy(loginPasswordVisible = !it.loginPasswordVisible) }

    fun onSignupEmailChanged(value: String) =
        _uiState.update { it.copy(signupEmail = value, signupEmailError = null, errorMessage = null) }

    fun onSignupPasswordChanged(value: String) =
        _uiState.update { it.copy(signupPassword = value, signupPasswordError = null, errorMessage = null) }

    fun onSignupConfirmPasswordChanged(value: String) =
        _uiState.update { it.copy(signupConfirmPassword = value, signupConfirmError = null, errorMessage = null) }

    fun onSignupPasswordVisibilityToggle() =
        _uiState.update { it.copy(signupPasswordVisible = !it.signupPasswordVisible) }

    fun onSignupConfirmVisibilityToggle() =
        _uiState.update { it.copy(signupConfirmVisible = !it.signupConfirmVisible) }

    fun onResetEmailChanged(value: String) =
        _uiState.update { it.copy(resetEmail = value, resetEmailError = null, errorMessage = null) }

    fun clearMessages() =
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }

    // ── Auth operations ───────────────────────────────────────────────────────

    /** Submits the login form; validates fields before calling the use case. */
    fun login() {
        val state = _uiState.value
        if (!validateLoginForm(state)) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, isNewSignup = false) }
            loginUseCase(state.loginEmail, state.loginPassword)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isAuthenticated = true, isNewSignup = false) }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading    = false,
                            errorMessage = mapAuthError(e.message)
                        )
                    }
                }
        }
    }

    /** Submits the signup form; validates fields before calling the use case. */
    fun signup() {
        val state = _uiState.value
        if (!validateSignupForm(state)) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, isNewSignup = false) }
            signupUseCase(state.signupEmail, state.signupPassword, state.signupConfirmPassword)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading      = false,
                            isAuthenticated = true,
                            isNewSignup     = true,
                            successMessage = "Account created successfully!"
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading    = false,
                            errorMessage = mapAuthError(e.message)
                        )
                    }
                }
        }
    }

    /** Signs the user out and resets UI state to unauthenticated. */
    fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            logoutUseCase()
            _uiState.value = AuthUiState(isAuthenticated = false)
        }
    }

    /** Sends a password-reset email and shows a success confirmation. */
    fun sendPasswordReset() {
        val state = _uiState.value
        if (!validateResetForm(state)) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            forgotPasswordUseCase(state.resetEmail)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading      = false,
                            resetEmailSent = true,
                            successMessage = "Password reset email sent. Check your inbox."
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading    = false,
                            errorMessage = mapAuthError(e.message)
                        )
                    }
                }
        }
    }

    // ── Field-level validation ────────────────────────────────────────────────

    private fun validateLoginForm(state: AuthUiState): Boolean {
        var valid = true
        val emailError = when {
            state.loginEmail.isBlank()                                                       -> "Email is required"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(state.loginEmail.trim()).matches()  -> "Enter a valid email"
            else                                                                              -> null
        }
        val passwordError = when {
            state.loginPassword.isBlank() -> "Password is required"
            state.loginPassword.length < 6 -> "At least 6 characters"
            else                           -> null
        }
        if (emailError != null || passwordError != null) valid = false
        _uiState.update { it.copy(loginEmailError = emailError, loginPasswordError = passwordError) }
        return valid
    }

    private fun validateSignupForm(state: AuthUiState): Boolean {
        var valid = true
        val emailError = when {
            state.signupEmail.isBlank()                                                      -> "Email is required"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(state.signupEmail.trim()).matches() -> "Enter a valid email"
            else                                                                              -> null
        }
        val passwordError = when {
            state.signupPassword.isBlank()  -> "Password is required"
            state.signupPassword.length < 6  -> "At least 6 characters"
            else                             -> null
        }
        val confirmError = when {
            state.signupConfirmPassword.isBlank()              -> "Please confirm your password"
            state.signupConfirmPassword != state.signupPassword -> "Passwords do not match"
            else                                                -> null
        }
        if (emailError != null || passwordError != null || confirmError != null) valid = false
        _uiState.update {
            it.copy(
                signupEmailError    = emailError,
                signupPasswordError = passwordError,
                signupConfirmError  = confirmError
            )
        }
        return valid
    }

    private fun validateResetForm(state: AuthUiState): Boolean {
        val emailError = when {
            state.resetEmail.isBlank()                                                      -> "Email is required"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(state.resetEmail.trim()).matches() -> "Enter a valid email"
            else                                                                             -> null
        }
        _uiState.update { it.copy(resetEmailError = emailError) }
        return emailError == null
    }

    // ── Error mapping ─────────────────────────────────────────────────────────

    /**
     * Maps Supabase error messages to user-friendly strings.
     * Supabase returns raw API error bodies; we sanitise them here.
     */
    private fun mapAuthError(raw: String?): String = when {
        raw == null                                        -> "An unexpected error occurred"
        raw.contains("invalid_credentials", ignoreCase = true) ||
            raw.contains("Invalid login",   ignoreCase = true)  -> "Incorrect email or password"
        raw.contains("already registered",  ignoreCase = true) ||
            raw.contains("User already",    ignoreCase = true)  -> "An account with this email already exists"
        raw.contains("rate limit",          ignoreCase = true)  -> "Too many attempts. Please wait and try again"
        raw.contains("network",             ignoreCase = true) ||
            raw.contains("Unable to resolve", ignoreCase = true) -> "No internet connection. Please check your network"
        raw.contains("Email not confirmed", ignoreCase = true)  -> "Please verify your email before signing in"
        else                                                     -> "Something went wrong. Please try again"
    }
}
