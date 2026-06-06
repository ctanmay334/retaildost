package com.example.di

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.BuildConfig
import com.example.data.auth.AuthRepository
import com.example.data.auth.AuthRepositoryImpl
import com.example.data.auth.SessionManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.functions.Functions
import io.ktor.client.engine.android.Android
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Provides the singleton Supabase client configured with
     * Auth, Postgrest, and Storage plugins.
     * URL and anon key are injected from BuildConfig (secrets.properties / .env).
     */
    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        val rawUrl = BuildConfig.SUPABASE_URL
        val cleanUrl = if (rawUrl.startsWith("http")) {
            rawUrl.trim().removeSuffix("/").removeSuffix("/rest/v1")
        } else {
            rawUrl.trim()
        }

        android.util.Log.d("AppModule", "Initializing Supabase with URL: $cleanUrl")

        return createSupabaseClient(
            supabaseUrl  = cleanUrl,
            supabaseKey  = BuildConfig.SUPABASE_ANON_KEY.trim()
        ) {
            install(Auth) {
                sessionManager = io.github.jan.supabase.auth.MemorySessionManager()
                codeVerifierCache = io.github.jan.supabase.auth.MemoryCodeVerifierCache()
            }
            install(Postgrest)
            install(Storage)
            install(Functions)
        }
    }

    /**
     * Provides EncryptedSharedPreferences backed by AES256-SIV keys.
     * Used by SessionManager to securely store JWT access/refresh tokens.
     * Falls back to regular SharedPreferences if EncryptedSharedPreferences
     * fails (known issue with security-crypto alpha on some devices).
     */
    @Provides
    @Singleton
    fun provideEncryptedSharedPreferences(
        @ApplicationContext context: Context
    ): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                "retaildost_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Throwable) {
            android.util.Log.e("AppModule", "EncryptedSharedPreferences failed, using fallback", e)
            context.getSharedPreferences("retaildost_secure_prefs_fallback", Context.MODE_PRIVATE)
        }
    }

    /**
     * Provides SessionManager as a singleton — all auth state lives here.
     */
    @Provides
    @Singleton
    fun provideSessionManager(
        supabaseClient: SupabaseClient,
        encryptedPrefs: SharedPreferences
    ): SessionManager = SessionManager(supabaseClient, encryptedPrefs)

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): com.example.data.db.AppDatabase = com.example.data.db.AppDatabase.getDatabase(context)

    @Provides
    @Singleton
    fun provideKiranaDao(
        database: com.example.data.db.AppDatabase
    ): com.example.data.dao.KiranaDao = database.kiranaDao()

    @Provides
    @Singleton
    fun provideInventoryDao(
        database: com.example.data.db.AppDatabase
    ): com.example.data.dao.InventoryDao = database.inventoryDao()

    @Provides
    @Singleton
    fun provideSaleDao(
        database: com.example.data.db.AppDatabase
    ): com.example.data.dao.SaleDao = database.saleDao()

    @Provides
    @Singleton
    fun provideKhataDao(
        database: com.example.data.db.AppDatabase
    ): com.example.data.dao.KhataDao = database.khataDao()

    @Provides
    @Singleton
    fun provideAlertDao(
        database: com.example.data.db.AppDatabase
    ): com.example.data.dao.AlertDao = database.alertDao()

    @Provides
    @Singleton
    fun provideDistributorDao(
        database: com.example.data.db.AppDatabase
    ): com.example.data.dao.DistributorDao = database.distributorDao()

    @Provides
    @Singleton
    fun provideOfflineQueueDao(
        database: com.example.data.db.AppDatabase
    ): com.example.data.dao.OfflineQueueDao = database.offlineQueueDao()

    @Provides
    @Singleton
    fun provideAnalyticsDao(
        database: com.example.data.db.AppDatabase
    ): com.example.data.dao.AnalyticsDao = database.analyticsDao()
}

/**
 * Separate module for interface→impl bindings (requires @Binds, not @Provides).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: com.example.data.auth.AuthRepositoryImpl): com.example.data.auth.AuthRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: com.example.data.repository.ProfileRepositoryImpl): com.example.data.repository.ProfileRepository

    @Binds
    @Singleton
    abstract fun bindInventoryRepository(impl: com.example.data.repository.InventoryRepositoryImpl): com.example.data.repository.InventoryRepository

    @Binds
    @Singleton
    abstract fun bindSaleRepository(impl: com.example.data.repository.SaleRepositoryImpl): com.example.data.repository.SaleRepository

    @Binds
    @Singleton
    abstract fun bindKhataRepository(impl: com.example.data.repository.KhataRepositoryImpl): com.example.data.repository.KhataRepository

    @Binds
    @Singleton
    abstract fun bindAlertRepository(impl: com.example.data.repository.AlertRepositoryImpl): com.example.data.repository.AlertRepository

    @Binds
    @Singleton
    abstract fun bindDistributorRepository(impl: com.example.data.repository.DistributorRepositoryImpl): com.example.data.repository.DistributorRepository

    @Binds
    @Singleton
    abstract fun bindOfflineQueueRepository(impl: com.example.data.repository.OfflineQueueRepositoryImpl): com.example.data.repository.OfflineQueueRepository

    @Binds
    @Singleton
    abstract fun bindAnalyticsRepository(impl: com.example.data.repository.AnalyticsRepositoryImpl): com.example.data.repository.AnalyticsRepository

    @Binds
    @Singleton
    abstract fun bindOcrScannerRepository(impl: com.example.data.repository.OcrScannerRepositoryImpl): com.example.data.repository.OcrScannerRepository
}
