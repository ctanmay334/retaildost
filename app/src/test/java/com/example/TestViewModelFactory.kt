package com.example

import android.app.Application
import android.content.Context
import com.example.data.auth.AuthRepository
import com.example.data.auth.SessionManager
import com.example.data.repository.*
import com.example.data.supabase.SupabaseManager
import com.example.sync.SyncScheduler
import com.example.ui.KiranaViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.functions.Functions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.lang.reflect.Proxy
import java.lang.reflect.InvocationHandler

object TestViewModelFactory {

    inline fun <reified T> mockInterface(): T {
        return Proxy.newProxyInstance(
            T::class.java.classLoader,
            arrayOf(T::class.java),
            InvocationHandler { _, method, _ ->
                when (method.returnType) {
                    Boolean::class.java, java.lang.Boolean::class.java -> false
                    Int::class.java, java.lang.Integer::class.java -> 0
                    Long::class.java, java.lang.Long::class.java -> 0L
                    Double::class.java, java.lang.Double::class.java -> 0.0
                    Float::class.java, java.lang.Float::class.java -> 0.0f
                    String::class.java -> ""
                    Result::class.java -> Result.success(Unit)
                    Flow::class.java -> {
                        val genericType = method.genericReturnType
                        var returnedValue: Any = emptyList<Any>()
                        if (genericType is java.lang.reflect.ParameterizedType) {
                            val innerType = genericType.actualTypeArguments.firstOrNull()
                            if (innerType != null) {
                                val innerClass = when (innerType) {
                                    is Class<*> -> innerType
                                    is java.lang.reflect.ParameterizedType -> innerType.rawType as? Class<*>
                                    else -> null
                                }
                                if (innerClass != null) {
                                    val name = innerClass.name
                                    if (name.contains("Integer") || name.contains("int")) {
                                        returnedValue = 0
                                    } else if (name.contains("Long")) {
                                        returnedValue = 0L
                                    } else if (name.contains("Boolean")) {
                                        returnedValue = false
                                    } else if (List::class.java.isAssignableFrom(innerClass)) {
                                        returnedValue = emptyList<Any>()
                                    }
                                }
                            }
                        }
                        flowOf(returnedValue)
                    }
                    else -> null
                }
            }
        ) as T
    }

    fun create(application: Application): KiranaViewModel {
        val supabaseClient = createSupabaseClient("https://example.supabase.co", "key") {
            install(Auth) {
                sessionManager = io.github.jan.supabase.auth.MemorySessionManager()
                codeVerifierCache = io.github.jan.supabase.auth.MemoryCodeVerifierCache()
            }
            install(Postgrest)
            install(Storage)
            install(Functions)
        }
        val sharedPrefs = application.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
        val sessionManager = SessionManager(supabaseClient, sharedPrefs)
        val syncScheduler = SyncScheduler(application)
        val supabaseManager = SupabaseManager(supabaseClient, sessionManager)

        return KiranaViewModel(
            application = application,
            sessionManager = sessionManager,
            authRepository = mockInterface<AuthRepository>(),
            inventoryRepository = mockInterface<InventoryRepository>(),
            profileRepository = mockInterface<ProfileRepository>(),
            saleRepository = mockInterface<SaleRepository>(),
            alertRepository = mockInterface<AlertRepository>(),
            distributorRepository = mockInterface<DistributorRepository>(),
            offlineQueueRepository = mockInterface<OfflineQueueRepository>(),
            syncScheduler = syncScheduler,
            ocrScannerRepository = mockInterface<OcrScannerRepository>(),
            supabaseClient = supabaseClient,
            khataRepository = mockInterface<KhataRepository>(),
            supabaseManager = supabaseManager
        )
    }
}
