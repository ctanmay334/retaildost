package com.example.data.repository

import com.example.data.model.ProfileEntity

/**
 * ProfileRepository
 * ─────────────────
 * Interface governing profile actions, coordinating local Room DB cache
 * and remote Supabase server records for offline-safety.
 */
interface ProfileRepository {

    /**
     * Gets the profile for [userId].
     * Fetches locally from Room first, then tries to update from Supabase if online.
     */
    suspend fun getProfile(userId: String): Result<ProfileEntity?>

    /**
     * Saves or updates the profile.
     * Writes to Room immediately, then attempts to update remote Supabase profiles table.
     */
    suspend fun saveProfile(profile: ProfileEntity): Result<Unit>

    /** Clears the local profile cache (called on logout). */
    suspend fun clearLocalProfile(): Unit
}
