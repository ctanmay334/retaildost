package com.example.data.repository

import android.util.Log
import com.example.data.dao.KiranaDao
import com.example.data.model.ProfileDto
import com.example.data.model.ProfileEntity
import com.example.data.model.ProfileUpdate
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val TAG = "ProfileRepositoryImpl"

/**
 * ProfileRepositoryImpl
 * ──────────────────────
 * Production implementation of [ProfileRepository] combining local Room database
 * caching and remote Supabase syncing.
 */
class ProfileRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val kiranaDao: KiranaDao
) : ProfileRepository {

    override suspend fun getProfile(userId: String): Result<ProfileEntity?> =
        withContext(Dispatchers.IO) {
            runCatching {
                // 1. Check local Room cache first
                val cached = kiranaDao.getProfileById(userId)

                // 2. Attempt to fetch fresh data from remote Supabase
                try {
                    val postgrest = supabaseClient.postgrest
                    val response = postgrest["profiles"].select {
                        filter {
                            eq("id", userId)
                        }
                    }
                    if (response.data != "[]" && response.data.isNotBlank()) {
                        val dto = response.decodeSingleOrNull<ProfileDto>()
                        if (dto != null) {
                            // If local cached profile has a newer/non-null onboardedAt, keep it!
                            val finalOnboardedAt = if (cached?.onboardedAt != null && dto.onboardedAt == null) {
                                cached.onboardedAt
                            } else {
                                dto.onboardedAt
                            }
                            
                            val finalStoreId = if ((dto.storeId.isBlank() || dto.storeId == "") && cached != null) {
                                cached.storeId
                            } else {
                                dto.storeId
                            }

                            val finalPlan = if (cached?.plan == "pro" && dto.plan != "pro") {
                                cached.plan
                            } else {
                                dto.plan
                            }

                            // Map DTO to Entity
                            val entity = ProfileEntity(
                                id = dto.id,
                                storeId = finalStoreId,
                                ownerName = if (dto.ownerName.isNullOrBlank()) cached?.ownerName ?: "" else dto.ownerName,
                                storeName = if (dto.storeName.isNullOrBlank()) cached?.storeName ?: "" else dto.storeName,
                                phone = dto.phone ?: cached?.phone,
                                pincode = if (dto.pincode.isNullOrBlank()) cached?.pincode ?: "" else dto.pincode,
                                city = if (dto.city.isNullOrBlank()) cached?.city ?: "" else dto.city,
                                state = if (dto.state.isNullOrBlank()) cached?.state ?: "" else dto.state,
                                businessType = if (dto.businessType.isNullOrBlank()) cached?.businessType ?: "" else dto.businessType,
                                plan = finalPlan,
                                onboardedAt = finalOnboardedAt,
                                updatedAt = System.currentTimeMillis()
                            )
                            // 3. Cache in Room DB
                            kiranaDao.insertProfile(entity)
                            return@runCatching entity
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to fetch profile from Supabase (offline mode?); returning local cache", e)
                }

                cached
            }.onFailure { e ->
                Log.e(TAG, "Error in getProfile", e)
            }
        }

    override suspend fun saveProfile(profile: ProfileEntity): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                // 1. Save to local Room cache immediately (offline-safe)
                kiranaDao.insertProfile(profile)

                // 2. Try upserting remote Supabase database
                try {
                    val dto = ProfileDto(
                        id = profile.id,
                        storeId = profile.storeId,
                        ownerName = profile.ownerName,
                        storeName = profile.storeName,
                        phone = profile.phone,
                        pincode = profile.pincode,
                        city = profile.city,
                        state = profile.state,
                        businessType = profile.businessType,
                        plan = profile.plan,
                        onboardedAt = profile.onboardedAt
                    )
                    val postgrest = supabaseClient.postgrest
                    postgrest["profiles"].upsert(dto)
                    Log.i(TAG, "Successfully upserted profile on Supabase")
                } catch (e: Exception) {
                    // Suppress remote error to allow offline-first completion
                    Log.e(TAG, "Failed to update profile remote on Supabase (cached locally)", e)
                }
                Unit
            }.onFailure { e ->
                Log.e(TAG, "Error saving profile", e)
            }
        }

    override suspend fun clearLocalProfile() {
        withContext(Dispatchers.IO) {
            try {
                kiranaDao.clearProfile()
                Log.d(TAG, "Cleared local profile database table")
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing local profile cache", e)
            }
        }
    }
}
