package com.voiceledger.ghana.offline

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.*

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