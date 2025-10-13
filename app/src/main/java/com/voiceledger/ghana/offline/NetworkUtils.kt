package com.voiceledger.ghana.offline

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Utility class for network connectivity detection and monitoring
 * Provides real-time network status updates and connection quality assessment
 */
object NetworkUtils {
    
    private val _networkState = MutableStateFlow(NetworkState())
    val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()
    
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    
    /**
     * Initialize network monitoring
     */
    fun initialize(context: Context) {
        connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        startNetworkMonitoring()
        updateNetworkState()
    }
    
    /**
     * Check if network is available
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.isConnected == true
        }
    }
    
    /**
     * Check if network is metered (mobile data)
     */
    fun isNetworkMetered(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return connectivityManager.isActiveNetworkMetered
    }
    
    /**
     * Get network type
     */
    fun getNetworkType(context: Context): NetworkType {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return NetworkType.NONE
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkType.NONE
            
            return when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.MOBILE
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
                else -> NetworkType.OTHER
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo ?: return NetworkType.NONE
            
            return when (networkInfo.type) {
                ConnectivityManager.TYPE_WIFI -> NetworkType.WIFI
                ConnectivityManager.TYPE_MOBILE -> NetworkType.MOBILE
                ConnectivityManager.TYPE_ETHERNET -> NetworkType.ETHERNET
                else -> NetworkType.OTHER
            }
        }
    }
    
    /**
     * Estimate network quality based on connection type and capabilities
     */
    fun estimateNetworkQuality(context: Context): NetworkQuality {
        if (!isNetworkAvailable(context)) return NetworkQuality.NONE
        
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return NetworkQuality.NONE
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkQuality.NONE
            
            return when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    // WiFi is generally good quality
                    NetworkQuality.GOOD
                }
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    // Estimate based on cellular capabilities
                    when {
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) -> NetworkQuality.GOOD
                        else -> NetworkQuality.FAIR // Assume mobile data is fair quality
                    }
                }
                else -> NetworkQuality.FAIR
            }
        } else {
            // Fallback for older Android versions
            return when (getNetworkType(context)) {
                NetworkType.WIFI -> NetworkQuality.GOOD
                NetworkType.MOBILE -> NetworkQuality.FAIR
                NetworkType.ETHERNET -> NetworkQuality.EXCELLENT
                else -> NetworkQuality.POOR
            }
        }
    }
    
    /**
     * Start monitoring network changes
     */
    private fun startNetworkMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    updateNetworkState()
                }
                
                override fun onLost(network: Network) {
                    updateNetworkState()
                }
                
                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    updateNetworkState()
                }
            }
            
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            
            connectivityManager?.registerNetworkCallback(networkRequest, networkCallback!!)
        }
    }
    
    /**
     * Update network state
     */
    private fun updateNetworkState() {
        connectivityManager?.let { cm ->
            val context = cm as? Context ?: return
            
            _networkState.value = NetworkState(
                isAvailable = isNetworkAvailable(context),
                isMetered = isNetworkMetered(context),
                networkType = getNetworkType(context),
                networkQuality = estimateNetworkQuality(context),
                lastUpdate = System.currentTimeMillis()
            )
        }
    }
    
    /**
     * Stop network monitoring
     */
    fun cleanup() {
        networkCallback?.let { callback ->
            connectivityManager?.unregisterNetworkCallback(callback)
        }
        networkCallback = null
        connectivityManager = null
    }
    
    /**
     * Check if network is suitable for sync operations
     */
    fun isSuitableForSync(context: Context, requireUnmetered: Boolean = false): Boolean {
        if (!isNetworkAvailable(context)) return false
        
        if (requireUnmetered && isNetworkMetered(context)) return false
        
        val quality = estimateNetworkQuality(context)
        return quality != NetworkQuality.NONE && quality != NetworkQuality.POOR
    }
    
    /**
     * Get recommended sync strategy based on network conditions
     */
    fun getRecommendedSyncStrategy(context: Context): SyncStrategy {
        if (!isNetworkAvailable(context)) return SyncStrategy.OFFLINE_ONLY
        
        val isMetered = isNetworkMetered(context)
        val quality = estimateNetworkQuality(context)
        
        return when {
            !isMetered && quality == NetworkQuality.EXCELLENT -> SyncStrategy.FULL_SYNC
            !isMetered && quality == NetworkQuality.GOOD -> SyncStrategy.NORMAL_SYNC
            isMetered && quality >= NetworkQuality.FAIR -> SyncStrategy.MINIMAL_SYNC
            else -> SyncStrategy.CRITICAL_ONLY
        }
    }
}

/**
 * Network state data class
 */
data class NetworkState(
    val isAvailable: Boolean = false,
    val isMetered: Boolean = false,
    val networkType: NetworkType = NetworkType.NONE,
    val networkQuality: NetworkQuality = NetworkQuality.NONE,
    val lastUpdate: Long = 0L
)

/**
 * Network types
 */
enum class NetworkType {
    NONE,
    WIFI,
    MOBILE,
    ETHERNET,
    OTHER
}

/**
 * Network quality levels
 */
enum class NetworkQuality {
    NONE,
    POOR,
    FAIR,
    GOOD,
    EXCELLENT
}

/**
 * Sync strategies based on network conditions
 */
enum class SyncStrategy {
    OFFLINE_ONLY,      // No network - queue operations
    CRITICAL_ONLY,     // Poor network - only critical operations
    MINIMAL_SYNC,      // Metered network - minimal data usage
    NORMAL_SYNC,       // Good network - normal sync operations
    FULL_SYNC          // Excellent network - full sync with media
}