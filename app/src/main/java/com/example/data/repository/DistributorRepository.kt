package com.example.data.repository

import com.example.data.model.DistributorDto
import com.example.data.model.DistributorEntity
import kotlinx.coroutines.flow.Flow

/**
 * DistributorRepository
 * ─────────────────────
 * Interface governing distributor catalog operations, including remote synchronization
 * from Supabase, local Room database caching, and registration of new suppliers.
 */
interface DistributorRepository {

    /** Live flow of locally cached distributors */
    val allDistributors: Flow<List<DistributorEntity>>

    /** Pulls all verified/active distributors from Supabase and caches them in Room */
    suspend fun fetchRemoteDistributors(): Result<Unit>

    /** Registers a new supplier in the remote database and caches them locally */
    suspend fun registerDistributor(distributor: DistributorDto): Result<Unit>
}
