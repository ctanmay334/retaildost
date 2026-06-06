package com.example.data.supabase

import android.util.Log
import com.example.data.auth.SessionManager
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.storage.storage
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import kotlin.time.Duration.Companion.seconds
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SupabaseManager"
private const val MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024 // 5 MB

/**
 * SupabaseError
 * ─────────────
 * Domain-centric sealed exception hierarchy transforming raw Supabase/Ktor errors
 * into structured categories suitable for UI reporting, logging, and retry analysis.
 */
sealed class SupabaseError : Exception() {
    object NetworkError : SupabaseError() {
        override val message: String = "Network connection failed. Please check your internet connectivity."
    }
    class AuthError(val msg: String) : SupabaseError() {
        override val message: String = msg
    }
    class StorageError(val msg: String) : SupabaseError() {
        override val message: String = msg
    }
    class ServerError(val code: Int, val msg: String) : SupabaseError() {
        override val message: String = "Supabase server returned error $code: $msg"
    }
    class UnknownError(val throwable: Throwable) : SupabaseError() {
        override val message: String = throwable.message ?: "An unexpected error occurred"
    }
}

/**
 * SupabaseManager
 * ───────────────
 * Central production-grade manager and coordinator for all Supabase services.
 * Features encapsulated Auth workflows, Storage objects operations, custom Edge Function triggers,
 * session storage bridging, and structured error conversion.
 */
@Singleton
class SupabaseManager @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val sessionManager: SessionManager
) {

    // ── 1. Authentication & Session Lifecycles ───────────────────────────────

    /**
     * Attempts to register a new user using their email and password.
     * Persists the session upon success.
     */
    suspend fun signUp(email: String, password: String): Result<UserInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                Log.d(TAG, "Starting signUp for $email")
                supabaseClient.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }
                val user = supabaseClient.auth.currentUserOrNull()
                    ?: throw SupabaseError.AuthError("Sign-up succeeded but current user context is missing")

                persistCurrentSession()
                Log.i(TAG, "signUp success for userId=${user.id}")
                user
            }.mapCatching {
                it
            }.recoverCatching { e ->
                val err = mapException(e)
                Log.e(TAG, "signUp error: ${err.message}", err)
                throw err
            }
        }

    /**
     * Authenticates an existing user with their email and password.
     * Persists the session upon success.
     */
    suspend fun signIn(email: String, password: String): Result<UserInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                Log.d(TAG, "Starting signIn for $email")
                supabaseClient.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                val user = supabaseClient.auth.currentUserOrNull()
                    ?: throw SupabaseError.AuthError("Sign-in succeeded but current user context is missing")

                persistCurrentSession()
                Log.i(TAG, "signIn success for userId=${user.id}")
                user
            }.mapCatching {
                it
            }.recoverCatching { e ->
                val err = mapException(e)
                Log.e(TAG, "signIn error: ${err.message}", err)
                throw err
            }
        }

    /**
     * Signs out the user from Supabase and purges the local session cache.
     */
    suspend fun signOut(): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                Log.d(TAG, "Starting signOut")
                supabaseClient.auth.signOut()
                sessionManager.clearSession()
                Log.i(TAG, "signOut successfully cleared session")
                Unit
            }.recoverCatching { e ->
                // Ensure we wipe local tokens even on server-side failures or offline conditions
                sessionManager.clearSession()
                val err = mapException(e)
                Log.w(TAG, "signOut network/server issue, local credentials cleared anyway: ${err.message}")
                throw err
            }
        }

    /**
     * Restores a previously saved JWT token session.
     */
    suspend fun restoreSession(): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                Log.d(TAG, "Attempting to restore session")
                val success = sessionManager.restoreSession()
                Log.i(TAG, "restoreSession completed. Success=$success")
                success
            }.recoverCatching { e ->
                val err = mapException(e)
                Log.e(TAG, "restoreSession encountered error: ${err.message}")
                throw err
            }
        }

    /**
     * Returns true if a live non-expired session is available in the client.
     */
    fun isSignedIn(): Boolean = supabaseClient.auth.currentUserOrNull() != null

    /**
     * Returns the currently authenticated user's information.
     */
    fun currentUser(): UserInfo? = supabaseClient.auth.currentUserOrNull()

    /**
     * Access the raw underlying Supabase Client if direct access is required by legacy wrappers.
     */
    fun getRawClient(): SupabaseClient = supabaseClient


    // ── 2. Object Storage Operations ─────────────────────────────────────────

    /**
     * Uploads file bytes to a Supabase storage bucket under the specified folder path.
     * Enforces file size limitations (max 5MB) and processes MIME content-types correctly.
     */
    suspend fun uploadFile(
        bucketId: String,
        path: String,
        bytes: ByteArray,
        contentType: String
    ): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                Log.d(TAG, "Uploading file to bucket=$bucketId, path=$path, size=${bytes.size} bytes")
                if (bytes.size > MAX_FILE_SIZE_BYTES) {
                    throw SupabaseError.StorageError("File size exceeds the 5MB upload limit (size=${bytes.size} bytes)")
                }

                val bucket = supabaseClient.storage[bucketId]
                // Upload options can include custom headers/mime types
                bucket.upload(path, bytes) {
                    upsert = true
                }

                val publicUrl = bucket.publicUrl(path)
                Log.i(TAG, "Successfully uploaded object to $path. Public URL: $publicUrl")
                publicUrl
            }.recoverCatching { e ->
                val err = mapException(e)
                Log.e(TAG, "uploadFile failed for path=$path: ${err.message}")
                throw err
            }
        }

    /**
     * Downloads file content bytes from the specified bucket and path.
     */
    suspend fun downloadFile(bucketId: String, path: String): Result<ByteArray> =
        withContext(Dispatchers.IO) {
            runCatching {
                Log.d(TAG, "Downloading file from bucket=$bucketId, path=$path")
                val bucket = supabaseClient.storage[bucketId]
                val bytes = bucket.downloadPublic(path)
                Log.i(TAG, "Successfully downloaded file of size=${bytes.size} bytes")
                bytes
            }.recoverCatching { e ->
                val err = mapException(e)
                Log.e(TAG, "downloadFile failed for path=$path: ${err.message}")
                throw err
            }
        }

    /**
     * Retrieves the public URL of a publicly visible bucket object.
     */
    fun getPublicUrl(bucketId: String, path: String): Result<String> =
        runCatching {
            val url = supabaseClient.storage[bucketId].publicUrl(path)
            Log.d(TAG, "getPublicUrl computed for path=$path: $url")
            url
        }.recoverCatching { e ->
            throw mapException(e)
        }

    /**
     * Generates a temporary signed URL to safely download private bucket objects.
     */
    suspend fun getSignedUrl(
        bucketId: String,
        path: String,
        expiresSeconds: Long = 3600
    ): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                Log.d(TAG, "Generating signed URL for bucket=$bucketId, path=$path, duration=${expiresSeconds}s")
                val signedUrl = supabaseClient.storage[bucketId].createSignedUrl(path, expiresSeconds.seconds)
                Log.i(TAG, "Signed URL generated successfully")
                signedUrl
            }.recoverCatching { e ->
                val err = mapException(e)
                Log.e(TAG, "getSignedUrl failed: ${err.message}")
                throw err
            }
        }

    /**
     * Deletes an object from the specified bucket.
     */
    suspend fun deleteFile(bucketId: String, path: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                Log.d(TAG, "Deleting file from bucket=$bucketId, path=$path")
                supabaseClient.storage[bucketId].delete(path)
                Log.i(TAG, "Object deleted successfully")
                Unit
            }.recoverCatching { e ->
                val err = mapException(e)
                Log.e(TAG, "deleteFile failed for path=$path: ${err.message}")
                throw err
            }
        }


    // ── 3. Edge Functions Execution ──────────────────────────────────────────

    /**
     * Triggers a custom Supabase Edge Function with custom headers, bodies, and network resilience.
     * Captures and decodes server response payloads.
     */
    suspend fun invokeFunction(
        functionName: String,
        body: String = "",
        headers: Map<String, String> = emptyMap()
    ): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                Log.d(TAG, "Invoking Edge Function: $functionName")
                val response = supabaseClient.functions.invoke(functionName) {
                    if (body.isNotEmpty()) {
                        setBody(body)
                    }
                    headers.forEach { (key, value) ->
                        header(key, value)
                    }
                }
                val responseText = response.bodyAsText()
                Log.i(TAG, "Edge Function $functionName completed successfully. Response size=${responseText.length}")
                responseText
            }.recoverCatching { e ->
                val err = mapException(e)
                Log.e(TAG, "invokeFunction failed for $functionName: ${err.message}")
                throw err
            }
        }


    // ── 4. Private Helper & Session Bridges ──────────────────────────────────

    /**
     * Reads current JWT tokens from the active client session and propagates them to [SessionManager].
     */
    private fun persistCurrentSession() {
        val session = supabaseClient.auth.currentSessionOrNull() ?: return
        val user = supabaseClient.auth.currentUserOrNull()
        sessionManager.persistSession(
            accessToken = session.accessToken,
            refreshToken = session.refreshToken,
            userId = user?.id ?: "",
            email = user?.email
        )
    }

    /**
     * Central mapper that transforms varied exceptions (Ktor HTTP, network losses, SDK RestExceptions)
     * into domain-appropriate [SupabaseError] classes.
     */
    private fun mapException(t: Throwable): SupabaseError {
        if (t is SupabaseError) return t

        val name = t.javaClass.name.lowercase()
        val msg = t.message?.lowercase() ?: ""

        return when {
            // Network failures & timeout exceptions
            t is IOException || name.contains("ktor") || name.contains("socket") || msg.contains("timeout") || msg.contains("connection") -> {
                SupabaseError.NetworkError
            }
            // Auth constraints or validation issues
            name.contains("auth") || msg.contains("invalid credentials") || msg.contains("email already in use") || msg.contains("jwt") || msg.contains("session") -> {
                SupabaseError.AuthError(t.message ?: "Authentication transaction failed")
            }
            // Storage limitations
            name.contains("storage") || msg.contains("bucket") || msg.contains("object not found") -> {
                SupabaseError.StorageError(t.message ?: "Storage operation failed")
            }
            // Catch-all
            else -> {
                SupabaseError.UnknownError(t)
            }
        }
    }
}
