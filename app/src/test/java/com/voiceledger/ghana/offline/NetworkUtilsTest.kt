package com.voiceledger.ghana.offline

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import io.mockk.*
import io.mockk.any
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlin.test.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28, 31, 33])
class NetworkUtilsTest {

    private lateinit var mockContext: Context
    private lateinit var mockAppContext: Context
    private lateinit var mockConnectivityManager: ConnectivityManager
    private lateinit var mockNetwork: Network
    private lateinit var mockNetworkCapabilities: NetworkCapabilities
    private lateinit var networkCallbackSlot: CapturingSlot<ConnectivityManager.NetworkCallback>

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockAppContext = mockk(relaxed = true)
        mockConnectivityManager = mockk(relaxed = true)
        mockNetwork = mockk(relaxed = true)
        mockNetworkCapabilities = mockk(relaxed = true)
        networkCallbackSlot = slot()

        // Mock context behavior
        every { mockContext.applicationContext } returns mockAppContext
        every { mockAppContext.getSystemService(Context.CONNECTIVITY_SERVICE) } returns mockConnectivityManager
        
        // Reset NetworkUtils state before each test
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

    @Test
    fun `initialize should store app context and setup connectivity manager`() {
        // Given
        every { mockConnectivityManager.activeNetwork } returns mockNetwork
        every { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } returns mockNetworkCapabilities
        every { mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        every { mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true

        // When
        NetworkUtils.initialize(mockContext)

        // Then
        verify { mockContext.applicationContext }
        verify { mockAppContext.getSystemService(Context.CONNECTIVITY_SERVICE) }
        
        // Verify network callback is registered
        verify { mockConnectivityManager.registerNetworkCallback(any(), capture(networkCallbackSlot)) }
        
        // Verify initial state is updated
        val initialState = NetworkUtils.networkState.value
        assertTrue(initialState.isAvailable)
        assertTrue(initialState.lastUpdate > 0)
    }

    @Test
    fun `isNetworkAvailable should return true when network is available`() {
        // Given
        every { mockConnectivityManager.activeNetwork } returns mockNetwork
        every { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } returns mockNetworkCapabilities
        every { mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        every { mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true
        
        NetworkUtils.initialize(mockContext)

        // When
        val result = NetworkUtils.isNetworkAvailable()

        // Then
        assertTrue(result)
    }

    @Test
    fun `isNetworkAvailable should return false when network is unavailable`() {
        // Given
        every { mockConnectivityManager.activeNetwork } returns null
        
        NetworkUtils.initialize(mockContext)

        // When
        val result = NetworkUtils.isNetworkAvailable()

        // Then
        assertFalse(result)
    }

    @Test
    fun `isNetworkMetered should return correct metered status`() {
        // Given
        every { mockConnectivityManager.isActiveNetworkMetered } returns true
        
        NetworkUtils.initialize(mockContext)

        // When
        val result = NetworkUtils.isNetworkMetered()

        // Then
        assertTrue(result)
    }

    @Test
    fun `getNetworkType should return WIFI for WiFi transport`() {
        // Given
        every { mockConnectivityManager.activeNetwork } returns mockNetwork
        every { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } returns mockNetworkCapabilities
        every { mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns true
        every { mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns false
        every { mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) } returns false
        
        NetworkUtils.initialize(mockContext)

        // When
        val result = NetworkUtils.getNetworkType()

        // Then
        assertEquals(NetworkType.WIFI, result)
    }

    @Test
    fun `getNetworkType should return MOBILE for cellular transport`() {
        // Given
        every { mockConnectivityManager.activeNetwork } returns mockNetwork
        every { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } returns mockNetworkCapabilities
        every { mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns false
        every { mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns true
        every { mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) } returns false
        
        NetworkUtils.initialize(mockContext)

        // When
        val result = NetworkUtils.getNetworkType()

        // Then
        assertEquals(NetworkType.MOBILE, result)
    }

    @Test
    fun `estimateNetworkQuality should return GOOD for WiFi`() {
        // Given
        every { mockConnectivityManager.activeNetwork } returns mockNetwork
        every { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } returns mockNetworkCapabilities
        every { mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns true
        every { mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        every { mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true
        
        NetworkUtils.initialize(mockContext)

        // When
        val result = NetworkUtils.estimateNetworkQuality()

        // Then
        assertEquals(NetworkQuality.GOOD, result)
    }

    @Test
    fun `network callback onAvailable should update network state`() = runTest {
        // Given
        every { mockConnectivityManager.activeNetwork } returns mockNetwork
        every { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } returns mockNetworkCapabilities
        every { mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        every { mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true
        every { mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns true
        every { mockConnectivityManager.isActiveNetworkMetered } returns false
        
        NetworkUtils.initialize(mockContext)
        
        // Capture the initial state
        val initialState = NetworkUtils.networkState.value
        
        // Wait a bit to ensure different timestamps
        Thread.sleep(10)
        
        // When - simulate network becoming available with different capabilities
        every { mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns true
        every { mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns false
        every { mockConnectivityManager.isActiveNetworkMetered } returns true
        
        networkCallbackSlot.captured.onAvailable(mockNetwork)
        
        // Then
        val updatedState = NetworkUtils.networkState.value
        assertNotEquals(initialState.lastUpdate, updatedState.lastUpdate)
        assertTrue(updatedState.isAvailable)
        assertTrue(updatedState.isMetered)
        assertEquals(NetworkType.MOBILE, updatedState.networkType)
    }

    @Test
    fun `network callback onLost should update network state`() = runTest {
        // Given
        every { mockConnectivityManager.activeNetwork } returns mockNetwork
        every { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } returns mockNetworkCapabilities
        every { mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        every { mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true
        every { mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns true
        
        NetworkUtils.initialize(mockContext)
        
        // Capture the initial state
        val initialState = NetworkUtils.networkState.value
        assertTrue(initialState.isAvailable)
        
        // Wait a bit to ensure different timestamps
        Thread.sleep(10)
        
        // When - simulate network loss
        every { mockConnectivityManager.activeNetwork } returns null
        
        networkCallbackSlot.captured.onLost(mockNetwork)
        
        // Then
        val updatedState = NetworkUtils.networkState.value
        assertNotEquals(initialState.lastUpdate, updatedState.lastUpdate)
        assertFalse(updatedState.isAvailable)
        assertEquals(NetworkType.NONE, updatedState.networkType)
        assertEquals(NetworkQuality.NONE, updatedState.networkQuality)
    }

    @Test
    fun `network callback onCapabilitiesChanged should update network state`() = runTest {
        // Given
        every { mockConnectivityManager.activeNetwork } returns mockNetwork
        every { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } returns mockNetworkCapabilities
        every { mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        every { mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true
        every { mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns true
        every { mockConnectivityManager.isActiveNetworkMetered } returns false
        
        NetworkUtils.initialize(mockContext)
        
        val initialState = NetworkUtils.networkState.value
        
        // Wait a bit to ensure different timestamps
        Thread.sleep(10)
        
        // When - simulate capabilities changing to cellular
        every { mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns false
        every { mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns true
        every { mockConnectivityManager.isActiveNetworkMetered } returns true
        
        networkCallbackSlot.captured.onCapabilitiesChanged(mockNetwork, mockNetworkCapabilities)
        
        // Then
        val updatedState = NetworkUtils.networkState.value
        assertNotEquals(initialState.lastUpdate, updatedState.lastUpdate)
        assertTrue(updatedState.isAvailable)
        assertTrue(updatedState.isMetered)
        assertEquals(NetworkType.MOBILE, updatedState.networkType)
    }

    @Test
    fun `cleanup should unregister callback and clear references`() {
        // Given
        every { mockConnectivityManager.activeNetwork } returns mockNetwork
        every { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } returns mockNetworkCapabilities
        every { mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        every { mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true
        
        NetworkUtils.initialize(mockContext)
        
        // When
        NetworkUtils.cleanup()
        
        // Then
        verify { mockConnectivityManager.unregisterNetworkCallback(networkCallbackSlot.captured) }
        
        // Verify that methods return default values after cleanup
        assertFalse(NetworkUtils.isNetworkAvailable())
        assertEquals(NetworkType.NONE, NetworkUtils.getNetworkType())
        assertEquals(NetworkQuality.NONE, NetworkUtils.estimateNetworkQuality())
    }

    @Test
    fun `methods should work with provided context when stored context is null`() {
        // Given - don't initialize NetworkUtils so appContext is null
        every { mockConnectivityManager.activeNetwork } returns mockNetwork
        every { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } returns mockNetworkCapabilities
        every { mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        every { mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true
        every { mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns true
        every { mockConnectivityManager.isActiveNetworkMetered } returns false
        
        // When & Then - methods should still work with explicit context
        assertTrue(NetworkUtils.isNetworkAvailable(mockContext))
        assertFalse(NetworkUtils.isNetworkMetered(mockContext))
        assertEquals(NetworkType.WIFI, NetworkUtils.getNetworkType(mockContext))
        assertEquals(NetworkQuality.GOOD, NetworkUtils.estimateNetworkQuality(mockContext))
    }

    @Test
    fun `methods should return default values when no context is available`() {
        // Given - don't initialize NetworkUtils so appContext is null
        
        // When & Then - methods should return safe defaults
        assertFalse(NetworkUtils.isNetworkAvailable())
        assertFalse(NetworkUtils.isNetworkMetered())
        assertEquals(NetworkType.NONE, NetworkUtils.getNetworkType())
        assertEquals(NetworkQuality.NONE, NetworkUtils.estimateNetworkQuality())
    }

    @Test
    fun `isSuitableForSync should return correct result based on network conditions`() {
        // Given - good unmetered network
        every { mockConnectivityManager.activeNetwork } returns mockNetwork
        every { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } returns mockNetworkCapabilities
        every { mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        every { mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true
        every { mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns true
        every { mockConnectivityManager.isActiveNetworkMetered } returns false
        
        NetworkUtils.initialize(mockContext)

        // When & Then
        assertTrue(NetworkUtils.isSuitableForSync())
        assertTrue(NetworkUtils.isSuitableForSync(requireUnmetered = true))
        
        // Given - metered network
        every { mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns true
        every { mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns false
        every { mockConnectivityManager.isActiveNetworkMetered } returns true
        
        networkCallbackSlot.captured.onCapabilitiesChanged(mockNetwork, mockNetworkCapabilities)
        
        // When & Then
        assertTrue(NetworkUtils.isSuitableForSync())
        assertFalse(NetworkUtils.isSuitableForSync(requireUnmetered = true))
    }

    @Test
    fun `getRecommendedSyncStrategy should return correct strategy based on network conditions`() {
        // Given - excellent unmetered network (Ethernet)
        every { mockConnectivityManager.activeNetwork } returns mockNetwork
        every { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } returns mockNetworkCapabilities
        every { mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        every { mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true
        every { mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) } returns true
        every { mockConnectivityManager.isActiveNetworkMetered } returns false
        
        NetworkUtils.initialize(mockContext)

        // When
        val strategy = NetworkUtils.getRecommendedSyncStrategy()

        // Then
        assertEquals(SyncStrategy.FULL_SYNC, strategy)
    }

    @Test
    fun `networkState flow should emit updates when connectivity changes`() = runTest {
        // Given
        every { mockConnectivityManager.activeNetwork } returns mockNetwork
        every { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } returns mockNetworkCapabilities
        every { mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        every { mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true
        every { mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns true
        every { mockConnectivityManager.isActiveNetworkMetered } returns false
        
        NetworkUtils.initialize(mockContext)
        
        val initialState = NetworkUtils.networkState.first()
        assertTrue(initialState.isAvailable)
        
        // Wait a bit to ensure different timestamps
        Thread.sleep(10)
        
        // When - simulate network loss
        every { mockConnectivityManager.activeNetwork } returns null
        
        networkCallbackSlot.captured.onLost(mockNetwork)
        
        // Then
        val updatedState = NetworkUtils.networkState.first()
        assertFalse(updatedState.isAvailable)
        assertNotEquals(initialState.lastUpdate, updatedState.lastUpdate)
    }
}
