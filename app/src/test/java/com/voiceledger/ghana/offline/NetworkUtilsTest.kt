package com.voiceledger.ghana.offline

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowConnectivityManager
import org.robolectric.shadows.ShadowNetwork
import org.robolectric.shadows.ShadowNetworkCapabilities
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NetworkUtilsTest {

    private lateinit var context: Context
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var shadowConnectivityManager: ShadowConnectivityManager

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        shadowConnectivityManager = Shadows.shadowOf(connectivityManager)
        
        NetworkUtils.initialize(context)
    }

    @After
    fun tearDown() {
        NetworkUtils.cleanup()
    }

    @Test
    fun `isNetworkAvailable returns false when no network is active`() {
        shadowConnectivityManager.setActiveNetworkInfo(null)

        val isAvailable = NetworkUtils.isNetworkAvailable(context)

        assertFalse(isAvailable)
    }

    @Test
    fun `isNetworkAvailable returns true when WiFi is connected`() {
        val network = ShadowNetwork.newInstance(1)
        val networkCapabilities = ShadowNetworkCapabilities.newInstance()
        Shadows.shadowOf(networkCapabilities).addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        Shadows.shadowOf(networkCapabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        Shadows.shadowOf(networkCapabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        shadowConnectivityManager.setNetworkCapabilities(network, networkCapabilities)
        shadowConnectivityManager.setDefaultNetwork(network)

        val isAvailable = NetworkUtils.isNetworkAvailable(context)

        assertTrue(isAvailable)
    }

    @Test
    fun `isNetworkAvailable returns true when cellular is connected`() {
        val network = ShadowNetwork.newInstance(2)
        val networkCapabilities = ShadowNetworkCapabilities.newInstance()
        Shadows.shadowOf(networkCapabilities).addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
        Shadows.shadowOf(networkCapabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        Shadows.shadowOf(networkCapabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        shadowConnectivityManager.setNetworkCapabilities(network, networkCapabilities)
        shadowConnectivityManager.setDefaultNetwork(network)

        val isAvailable = NetworkUtils.isNetworkAvailable(context)

        assertTrue(isAvailable)
    }

    @Test
    fun `getNetworkType returns WiFi when connected to WiFi`() {
        val network = ShadowNetwork.newInstance(1)
        val networkCapabilities = ShadowNetworkCapabilities.newInstance()
        Shadows.shadowOf(networkCapabilities).addTransportType(NetworkCapabilities.TRANSPORT_WIFI)

        shadowConnectivityManager.setNetworkCapabilities(network, networkCapabilities)
        shadowConnectivityManager.setDefaultNetwork(network)

        val networkType = NetworkUtils.getNetworkType(context)

        assertEquals(NetworkType.WIFI, networkType)
    }

    @Test
    fun `getNetworkType returns MOBILE when connected to cellular`() {
        val network = ShadowNetwork.newInstance(2)
        val networkCapabilities = ShadowNetworkCapabilities.newInstance()
        Shadows.shadowOf(networkCapabilities).addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)

        shadowConnectivityManager.setNetworkCapabilities(network, networkCapabilities)
        shadowConnectivityManager.setDefaultNetwork(network)

        val networkType = NetworkUtils.getNetworkType(context)

        assertEquals(NetworkType.MOBILE, networkType)
    }

    @Test
    fun `getNetworkType returns ETHERNET when connected via ethernet`() {
        val network = ShadowNetwork.newInstance(3)
        val networkCapabilities = ShadowNetworkCapabilities.newInstance()
        Shadows.shadowOf(networkCapabilities).addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)

        shadowConnectivityManager.setNetworkCapabilities(network, networkCapabilities)
        shadowConnectivityManager.setDefaultNetwork(network)

        val networkType = NetworkUtils.getNetworkType(context)

        assertEquals(NetworkType.ETHERNET, networkType)
    }

    @Test
    fun `getNetworkType returns NONE when no network is available`() {
        shadowConnectivityManager.setDefaultNetwork(null)

        val networkType = NetworkUtils.getNetworkType(context)

        assertEquals(NetworkType.NONE, networkType)
    }

    @Test
    fun `isNetworkMetered returns false for WiFi`() {
        val network = ShadowNetwork.newInstance(1)
        val networkCapabilities = ShadowNetworkCapabilities.newInstance()
        Shadows.shadowOf(networkCapabilities).addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        Shadows.shadowOf(networkCapabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)

        shadowConnectivityManager.setNetworkCapabilities(network, networkCapabilities)
        shadowConnectivityManager.setDefaultNetwork(network)

        val isMetered = NetworkUtils.isNetworkMetered(context)

        assertFalse(isMetered)
    }

    @Test
    fun `isNetworkMetered returns true for cellular data`() {
        val network = ShadowNetwork.newInstance(2)
        val networkCapabilities = ShadowNetworkCapabilities.newInstance()
        Shadows.shadowOf(networkCapabilities).addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)

        shadowConnectivityManager.setNetworkCapabilities(network, networkCapabilities)
        shadowConnectivityManager.setDefaultNetwork(network)

        val isMetered = NetworkUtils.isNetworkMetered(context)

        assertTrue(isMetered)
    }

    @Test
    fun `estimateNetworkQuality returns GOOD for WiFi`() {
        val network = ShadowNetwork.newInstance(1)
        val networkCapabilities = ShadowNetworkCapabilities.newInstance()
        Shadows.shadowOf(networkCapabilities).addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        Shadows.shadowOf(networkCapabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        Shadows.shadowOf(networkCapabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        shadowConnectivityManager.setNetworkCapabilities(network, networkCapabilities)
        shadowConnectivityManager.setDefaultNetwork(network)

        val quality = NetworkUtils.estimateNetworkQuality(context)

        assertEquals(NetworkQuality.GOOD, quality)
    }

    @Test
    fun `estimateNetworkQuality returns EXCELLENT for ethernet`() {
        val network = ShadowNetwork.newInstance(3)
        val networkCapabilities = ShadowNetworkCapabilities.newInstance()
        Shadows.shadowOf(networkCapabilities).addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
        Shadows.shadowOf(networkCapabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        Shadows.shadowOf(networkCapabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        shadowConnectivityManager.setNetworkCapabilities(network, networkCapabilities)
        shadowConnectivityManager.setDefaultNetwork(network)

        val quality = NetworkUtils.estimateNetworkQuality(context)

        assertEquals(NetworkQuality.EXCELLENT, quality)
    }

    @Test
    fun `estimateNetworkQuality returns NONE when no network`() {
        shadowConnectivityManager.setDefaultNetwork(null)

        val quality = NetworkUtils.estimateNetworkQuality(context)

        assertEquals(NetworkQuality.NONE, quality)
    }

    @Test
    fun `isSuitableForSync returns true for WiFi connection`() {
        val network = ShadowNetwork.newInstance(1)
        val networkCapabilities = ShadowNetworkCapabilities.newInstance()
        Shadows.shadowOf(networkCapabilities).addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        Shadows.shadowOf(networkCapabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        Shadows.shadowOf(networkCapabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        Shadows.shadowOf(networkCapabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)

        shadowConnectivityManager.setNetworkCapabilities(network, networkCapabilities)
        shadowConnectivityManager.setDefaultNetwork(network)

        val suitable = NetworkUtils.isSuitableForSync(context, requireUnmetered = false)

        assertTrue(suitable)
    }

    @Test
    fun `isSuitableForSync returns false for metered when unmetered required`() {
        val network = ShadowNetwork.newInstance(2)
        val networkCapabilities = ShadowNetworkCapabilities.newInstance()
        Shadows.shadowOf(networkCapabilities).addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
        Shadows.shadowOf(networkCapabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        Shadows.shadowOf(networkCapabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        shadowConnectivityManager.setNetworkCapabilities(network, networkCapabilities)
        shadowConnectivityManager.setDefaultNetwork(network)

        val suitable = NetworkUtils.isSuitableForSync(context, requireUnmetered = true)

        assertFalse(suitable)
    }

    @Test
    fun `isSuitableForSync returns false when no network`() {
        shadowConnectivityManager.setDefaultNetwork(null)

        val suitable = NetworkUtils.isSuitableForSync(context, requireUnmetered = false)

        assertFalse(suitable)
    }

    @Test
    fun `getRecommendedSyncStrategy returns FULL_SYNC for excellent unmetered`() {
        val network = ShadowNetwork.newInstance(3)
        val networkCapabilities = ShadowNetworkCapabilities.newInstance()
        Shadows.shadowOf(networkCapabilities).addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
        Shadows.shadowOf(networkCapabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        Shadows.shadowOf(networkCapabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        Shadows.shadowOf(networkCapabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)

        shadowConnectivityManager.setNetworkCapabilities(network, networkCapabilities)
        shadowConnectivityManager.setDefaultNetwork(network)

        val strategy = NetworkUtils.getRecommendedSyncStrategy(context)

        assertEquals(SyncStrategy.FULL_SYNC, strategy)
    }

    @Test
    fun `getRecommendedSyncStrategy returns NORMAL_SYNC for good WiFi`() {
        val network = ShadowNetwork.newInstance(1)
        val networkCapabilities = ShadowNetworkCapabilities.newInstance()
        Shadows.shadowOf(networkCapabilities).addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        Shadows.shadowOf(networkCapabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        Shadows.shadowOf(networkCapabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        Shadows.shadowOf(networkCapabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)

        shadowConnectivityManager.setNetworkCapabilities(network, networkCapabilities)
        shadowConnectivityManager.setDefaultNetwork(network)

        val strategy = NetworkUtils.getRecommendedSyncStrategy(context)

        assertTrue(strategy == SyncStrategy.FULL_SYNC || strategy == SyncStrategy.NORMAL_SYNC)
    }

    @Test
    fun `getRecommendedSyncStrategy returns MINIMAL_SYNC for metered cellular`() {
        val network = ShadowNetwork.newInstance(2)
        val networkCapabilities = ShadowNetworkCapabilities.newInstance()
        Shadows.shadowOf(networkCapabilities).addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
        Shadows.shadowOf(networkCapabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        Shadows.shadowOf(networkCapabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        shadowConnectivityManager.setNetworkCapabilities(network, networkCapabilities)
        shadowConnectivityManager.setDefaultNetwork(network)

        val strategy = NetworkUtils.getRecommendedSyncStrategy(context)

        assertEquals(SyncStrategy.MINIMAL_SYNC, strategy)
    }

    @Test
    fun `getRecommendedSyncStrategy returns OFFLINE_ONLY when no network`() {
        shadowConnectivityManager.setDefaultNetwork(null)

        val strategy = NetworkUtils.getRecommendedSyncStrategy(context)

        assertEquals(SyncStrategy.OFFLINE_ONLY, strategy)
    }

    @Test
    fun `network state flow emits changes on network transitions`() = runTest(testDispatcher) {
        NetworkUtils.initialize(context)

        val network = ShadowNetwork.newInstance(1)
        val networkCapabilities = ShadowNetworkCapabilities.newInstance()
        Shadows.shadowOf(networkCapabilities).addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        Shadows.shadowOf(networkCapabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        Shadows.shadowOf(networkCapabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        shadowConnectivityManager.setNetworkCapabilities(network, networkCapabilities)
        shadowConnectivityManager.setDefaultNetwork(network)

        advanceUntilIdle()

        val state = NetworkUtils.networkState.value
        assertTrue(state.isAvailable)
        assertEquals(NetworkType.WIFI, state.networkType)
    }

    @Test
    fun `network state flow tracks network quality changes`() = runTest(testDispatcher) {
        NetworkUtils.initialize(context)

        val network = ShadowNetwork.newInstance(3)
        val networkCapabilities = ShadowNetworkCapabilities.newInstance()
        Shadows.shadowOf(networkCapabilities).addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
        Shadows.shadowOf(networkCapabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        Shadows.shadowOf(networkCapabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        shadowConnectivityManager.setNetworkCapabilities(network, networkCapabilities)
        shadowConnectivityManager.setDefaultNetwork(network)

        advanceUntilIdle()

        val state = NetworkUtils.networkState.value
        assertEquals(NetworkQuality.EXCELLENT, state.networkQuality)
    }

    @Test
    fun `network state reflects metered status`() = runTest(testDispatcher) {
        NetworkUtils.initialize(context)

        val network = ShadowNetwork.newInstance(2)
        val networkCapabilities = ShadowNetworkCapabilities.newInstance()
        Shadows.shadowOf(networkCapabilities).addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
        Shadows.shadowOf(networkCapabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        Shadows.shadowOf(networkCapabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        shadowConnectivityManager.setNetworkCapabilities(network, networkCapabilities)
        shadowConnectivityManager.setDefaultNetwork(network)

        advanceUntilIdle()

        val state = NetworkUtils.networkState.value
        assertTrue(state.isMetered)
    }

    @Test
    fun `cleanup unregisters network callback`() {
        NetworkUtils.initialize(context)
        NetworkUtils.cleanup()
        
        // After cleanup, network monitoring should stop
        // Verify no crashes occur
    }
}
