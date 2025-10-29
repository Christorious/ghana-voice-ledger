package com.voiceledger.ghana.offline

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import io.mockk.any
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])
class NetworkUtilsTest {

    private lateinit var context: Context
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var network: Network
    private lateinit var networkCapabilities: NetworkCapabilities
    private val callbackSlot = slot<ConnectivityManager.NetworkCallback>()

    @Before
    fun setUp() {
        callbackSlot.clear()
        NetworkUtils.cleanup()

        context = mockk(relaxed = true)
        connectivityManager = mockk(relaxed = true)
        network = mockk(relaxed = true)
        networkCapabilities = mockk(relaxed = true)

        every { context.applicationContext } returns context
        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager

        every { connectivityManager.registerNetworkCallback(any<NetworkRequest>(), capture(callbackSlot)) } answers {}
        every { connectivityManager.unregisterNetworkCallback(any<ConnectivityManager.NetworkCallback>()) } answers {}

        every { connectivityManager.activeNetwork } returns null
        every { connectivityManager.getNetworkCapabilities(any()) } returns null
        every { connectivityManager.isActiveNetworkMetered } returns false

        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) } returns true
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns true
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns false
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) } returns false
    }

    @After
    fun tearDown() {
        NetworkUtils.cleanup()
        clearAllMocks()
    }

    @Test
    fun initializeShouldRegisterCallbackAndUpdateInitialState() {
        NetworkUtils.initialize(context)

        assertTrue(callbackSlot.isCaptured)
        val state = NetworkUtils.networkState.value
        assertFalse(state.isAvailable)
        assertEquals(NetworkType.NONE, state.networkType)
    }

    @Test
    fun networkCallbackShouldUpdateStateOnAvailabilityChanges() {
        NetworkUtils.initialize(context)

        val callback = callbackSlot.captured

        // Simulate network availability
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
        every { connectivityManager.isActiveNetworkMetered } returns false

        callback.onAvailable(network)

        val availableState = NetworkUtils.networkState.value
        assertTrue(availableState.isAvailable)
        assertEquals(NetworkType.WIFI, availableState.networkType)
        assertEquals(NetworkQuality.GOOD, availableState.networkQuality)

        // Simulate network loss
        every { connectivityManager.activeNetwork } returns null
        every { connectivityManager.getNetworkCapabilities(network) } returns null

        callback.onLost(network)

        val lostState = NetworkUtils.networkState.value
        assertFalse(lostState.isAvailable)
        assertEquals(NetworkType.NONE, lostState.networkType)
    }

    @Test
    fun cleanupShouldUnregisterCallbackAndReleaseResources() {
        NetworkUtils.initialize(context)
        val callback = callbackSlot.captured

        NetworkUtils.cleanup()

        verify { connectivityManager.unregisterNetworkCallback(callback) }
        assertFalse(NetworkUtils.isNetworkAvailable())
    }
}
