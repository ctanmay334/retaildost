package com.example.data.auth

import io.github.jan.supabase.auth.user.UserInfo
import com.example.data.model.ProfileDto

/**
 * AuthRepository
 * ──────────────
 * Clean-architecture boundary for all authentication operations.
 * Implementations use Supabase Auth; callers only see this interface.
 */
interface AuthRepository {

    /** Sign up a new user with email + password. Returns the created [UserInfo]. */
    suspend fun signUp(email: String, password: String): Result<UserInfo>

    /** Sign in an existing user. Returns the authenticated [UserInfo]. */
    suspend fun signIn(email: String, password: String): Result<UserInfo>

    /** Sign out the current user and clear all local session tokens. */
    suspend fun signOut(): Result<Unit>

    /** Send a password-reset email to [email]. */
    suspend fun sendPasswordReset(email: String): Result<Unit>

    /** Returns true if there is a valid, non-expired session in memory. */
    suspend fun hasActiveSession(): Boolean

    /** Returns the current user info, or null if not signed in. */
    suspend fun getCurrentUser(): UserInfo?

    /** Fetches the user profile from Supabase profiles table. */
    suspend fun fetchUserProfile(userId: String): Result<ProfileDto?>

    /** Updates the user profile with onboarding details. */
    suspend fun updateUserProfile(userId: String, ownerName: String, storeName: String, pincode: String): Result<Unit>
}
