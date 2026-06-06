package com.example.domain.auth

import com.example.data.auth.AuthRepository
import io.github.jan.supabase.auth.user.UserInfo
import javax.inject.Inject

// ── Use-case hierarchy ────────────────────────────────────────────────────────
// Each use-case is a single-method class that wraps one repository operation.
// This keeps the ViewModel thin and business logic independently testable.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Validates credentials and delegates to [AuthRepository.signIn].
 * Returns a [Result] so the ViewModel can handle success/error uniformly.
 */
class LoginUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<UserInfo> {
        val trimmedEmail = email.trim().lowercase()
        // Client-side validation (belt-and-suspenders; UI also validates)
        require(trimmedEmail.isNotBlank()) { "Email must not be blank" }
        require(password.length >= 6)       { "Password must be at least 6 characters" }
        return repository.signIn(trimmedEmail, password)
    }
}

/**
 * Validates signup fields and delegates to [AuthRepository.signUp].
 */
class SignupUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String, confirmPassword: String): Result<UserInfo> {
        val trimmedEmail = email.trim().lowercase()
        require(trimmedEmail.isNotBlank())       { "Email must not be blank" }
        require(android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            "Enter a valid email address"
        }
        require(password.length >= 6)             { "Password must be at least 6 characters" }
        require(password == confirmPassword)       { "Passwords do not match" }
        return repository.signUp(trimmedEmail, password)
    }
}

/**
 * Signs out the current user and clears all local session state.
 */
class LogoutUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(): Result<Unit> = repository.signOut()
}

/**
 * Sends a password-reset email via Supabase Auth.
 */
class ForgotPasswordUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String): Result<Unit> {
        val trimmedEmail = email.trim().lowercase()
        require(trimmedEmail.isNotBlank()) { "Email must not be blank" }
        require(android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            "Enter a valid email address"
        }
        return repository.sendPasswordReset(trimmedEmail)
    }
}
