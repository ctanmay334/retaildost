package com.example.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "NetworkObserver"

/**
 * NetworkObserver
 * ───────────────
 * Active internet monitoring manager utilizing Android's ConnectivityManager.
 * Detects dynamic switches to online states and dispatches background queue synchronization tasks immediately.
 */
@Singleton
class NetworkObserver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncScheduler: SyncScheduler
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            Log.i(TAG, "Network connection detected ACTIVE. Dispatching immediate queue sync.")
            syncScheduler.triggerImmediateSync()
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            Log.w(TAG, "Network connection went OFFLINE. Remote synchronizations paused.")
        }
    }

    /**
     * Binds the network observer callback.
     */
    fun startObserving() {
        Log.i(TAG, "Binding network connectivity observers...")
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        try {
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback listener: ${e.message}", e)
        }
    }

    /**
     * Unbinds the network observer callback.
     */
    fun stopObserving() {
        Log.i(TAG, "Unbinding network connectivity observers.")
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: IllegalArgumentException) {
            // Callback was already unregistered or not bound
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister network callback listener: ${e.message}", e)
        }
    }
}
