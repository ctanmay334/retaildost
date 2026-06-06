package com.example.data.auth

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import com.example.data.model.ProfileDto
import com.example.data.model.ProfileUpdate
import io.github.jan.supabase.postgrest.postgrest

private const val TAG = "AuthRepositoryImpl"

/**
 * AuthRepositoryImpl
 * ──────────────────
 * Supabase Auth backend implementation of [AuthRepository].
 *
 * All operations run on [Dispatchers.IO] via [withContext] to avoid
 * blocking the main thread. Every public method returns a [Result]
 * so callers never deal with raw exceptions.
 */
class AuthRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val sessionManager: SessionManager
) : AuthRepository {

    // ── Sign Up ──────────────────────────────────────────────────────────────

    override suspend fun signUp(email: String, password: String): Result<UserInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabaseClient.auth.signUpWith(Email) {
                    this.email    = email
                    this.password = password
                }
                val user = supabaseClient.auth.currentUserOrNull()
                    ?: error("Sign-up succeeded but user is null")

                // Persist session tokens immediately
                persistCurrentSession()
                Log.i(TAG, "signUp success: uid=${user.id}")
                user
            }.onFailure { e ->
                Log.e(TAG, "signUp failed", e)
            }
        }

    // ── Sign In ──────────────────────────────────────────────────────────────

    override suspend fun signIn(email: String, password: String): Result<UserInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabaseClient.auth.signInWith(Email) {
                    this.email    = email
                    this.password = password
                }
                val user = supabaseClient.auth.currentUserOrNull()
                    ?: error("Sign-in succeeded but user is null")

                persistCurrentSession()
                Log.i(TAG, "signIn success: uid=${user.id}")
                user
            }.onFailure { e ->
                Log.e(TAG, "signIn failed", e)
            }
        }

    // ── Sign Out ─────────────────────────────────────────────────────────────

    override suspend fun signOut(): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabaseClient.auth.signOut()
                sessionManager.clearSession()
                Log.i(TAG, "signOut success")
                Unit
            }.onFailure { e ->
                // Even on network error, clear local tokens so user is effectively logged out
                sessionManager.clearSession()
                Log.e(TAG, "signOut network error — local session cleared anyway", e)
            }
        }

    // ── Password Reset ────────────────────────────────────────────────────────

    override suspend fun sendPasswordReset(email: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabaseClient.auth.resetPasswordForEmail(email)
                Log.i(TAG, "Password reset email sent to $email")
                Unit
            }.onFailure { e ->
                Log.e(TAG, "sendPasswordReset failed", e)
            }
        }

    // ── Session queries ───────────────────────────────────────────────────────

    override suspend fun hasActiveSession(): Boolean =
        withContext(Dispatchers.IO) {
            supabaseClient.auth.currentUserOrNull() != null
        }

    override suspend fun getCurrentUser(): UserInfo? =
        withContext(Dispatchers.IO) {
            supabaseClient.auth.currentUserOrNull()
        }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Reads the live session from the Supabase client and persists
     * access + refresh tokens to EncryptedSharedPreferences via [SessionManager].
     */
    private fun persistCurrentSession() {
        val session = supabaseClient.auth.currentSessionOrNull() ?: return
        val user    = supabaseClient.auth.currentUserOrNull()
        sessionManager.persistSession(
            accessToken  = session.accessToken,
            refreshToken = session.refreshToken,
            userId       = user?.id ?: "",
            email        = user?.email
        )
    }

    override suspend fun fetchUserProfile(userId: String): Result<ProfileDto?> =
        withContext(Dispatchers.IO) {
            runCatching {
                val postgrest = supabaseClient.postgrest
                val response = postgrest["profiles"].select {
                    filter {
                        eq("id", userId)
                    }
                }
                if (response.data == "[]" || response.data.isBlank()) {
                    null
                } else {
                    response.decodeSingleOrNull<ProfileDto>()
                }
            }.onFailure { e ->
                Log.e(TAG, "fetchUserProfile failed", e)
            }
        }

    override suspend fun updateUserProfile(
        userId: String,
        ownerName: String,
        storeName: String,
        pincode: String
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val postgrest = supabaseClient.postgrest
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", java.util.Locale.US)
                val isoString = sdf.format(java.util.Date())
                
                // Try to retrieve existing store_id or other fields
                val existing = try {
                    val response = postgrest["profiles"].select {
                        filter {
                            eq("id", userId)
                        }
                    }
                    if (response.data != "[]" && response.data.isNotBlank()) {
                        response.decodeSingleOrNull<ProfileDto>()
                    } else null
                } catch (e: Exception) {
                    null
                }
                
                val storeId = existing?.storeId ?: java.util.UUID.randomUUID().toString()
                val phone = existing?.phone ?: ""
                
                val dto = ProfileDto(
                    id = userId,
                    storeId = storeId,
                    ownerName = ownerName,
                    storeName = storeName,
                    phone = phone,
                    pincode = pincode,
                    plan = "pro",
                    onboardedAt = isoString
                )
                
                postgrest["profiles"].upsert(dto)
                Unit
            }.onFailure { e ->
                Log.e(TAG, "updateUserProfile failed", e)
            }
        }
}
