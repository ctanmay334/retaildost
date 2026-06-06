package com.example.data.auth

import android.content.SharedPreferences
import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SessionManager"
private const val KEY_ACCESS_TOKEN  = "supabase_access_token"
private const val KEY_REFRESH_TOKEN = "supabase_refresh_token"
private const val KEY_USER_ID       = "supabase_user_id"
private const val KEY_USER_EMAIL    = "supabase_user_email"

/**
 * SessionManager
 * ──────────────
 * Single source of truth for JWT session state.
 *
 * Responsibilities:
 *  • Persist access + refresh tokens to [EncryptedSharedPreferences]
 *  • Restore session on cold-start via [restoreSession]
 *  • Clear tokens on sign-out
 *  • Expose current user helpers to avoid scattering auth checks across the app
 */
@Singleton
class SessionManager @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val encryptedPrefs: SharedPreferences
) {

    // ── Persistence helpers ──────────────────────────────────────────────────

    /** Saves access + refresh tokens after a successful auth event. */
    fun persistSession(accessToken: String, refreshToken: String, userId: String, email: String?) {
        encryptedPrefs.edit()
            .putString(KEY_ACCESS_TOKEN,  accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putString(KEY_USER_ID,       userId)
            .putString(KEY_USER_EMAIL,    email ?: "")
            .apply()
        Log.d(TAG, "Session persisted for userId=$userId")
    }

    /** Removes all stored tokens — called on logout. */
    fun clearSession() {
        encryptedPrefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_EMAIL)
            .apply()
        Log.d(TAG, "Session cleared")
    }

    /** Returns true if a saved access token exists on disk. */
    fun hasSavedSession(): Boolean =
        encryptedPrefs.getString(KEY_ACCESS_TOKEN, null) != null

    fun getSavedAccessToken():  String? = encryptedPrefs.getString(KEY_ACCESS_TOKEN,  null)
    fun getSavedRefreshToken(): String? = encryptedPrefs.getString(KEY_REFRESH_TOKEN, null)
    fun getSavedUserId():       String? = encryptedPrefs.getString(KEY_USER_ID,       null)
    fun getSavedUserEmail():    String? = encryptedPrefs.getString(KEY_USER_EMAIL,    null)

    // ── Session restoration ──────────────────────────────────────────────────

    /**
     * Attempts to restore a Supabase session from persisted tokens.
     * Called once at app start (from AuthViewModel.init).
     *
     * @return true if session was restored successfully
     */
    suspend fun restoreSession(): Boolean = withContext(Dispatchers.IO) {
        val accessToken  = getSavedAccessToken()  ?: return@withContext false
        val refreshToken = getSavedRefreshToken() ?: return@withContext false

        return@withContext try {
            // Import the saved tokens back into the Supabase client
            supabaseClient.auth.importSession(
                io.github.jan.supabase.auth.user.UserSession(
                    accessToken  = accessToken,
                    refreshToken = refreshToken,
                    expiresIn    = 3600L,
                    tokenType    = "bearer",
                    user         = null
                )
            )
            // Refresh to validate + get a fresh access token
            supabaseClient.auth.refreshCurrentSession()
            Log.d(TAG, "Session restored successfully")
            true
        } catch (e: Throwable) {
            Log.w(TAG, "Session restore failed — clearing stale tokens", e)
            clearSession()
            false
        }
    }

    // ── Current user helpers ─────────────────────────────────────────────────

    /** Returns current [UserInfo] from the live Supabase session, or null. */
    fun currentUser(): UserInfo? = supabaseClient.auth.currentUserOrNull()

    /** Returns true if there is a non-null current user in the Supabase client. */
    fun isSignedIn(): Boolean = currentUser() != null
}
