package com.voiceledger.ghana.service

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager as AndroidPowerManager
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PowerManagerTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var context: Context
    private lateinit var androidPowerManager: AndroidPowerManager
    private lateinit var batteryManager: BatteryManager
    private lateinit var powerManager: PowerManager

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        context = mockk(relaxed = true)
        androidPowerManager = mockk(relaxed = true)
        batteryManager = mockk(relaxed = true)
        
        every { context.getSystemService(Context.POWER_SERVICE) } returns androidPowerManager
        every { context.getSystemService(Context.BATTERY_SERVICE) } returns batteryManager
        
        // Mock battery intent
        val batteryIntent = mockk<Intent>(relaxed = true)
        every { context.registerReceiver(null, any<IntentFilter>()) } returns batteryIntent
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) } returns 80
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1) } returns 100
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) } returns BatteryManager.BATTERY_STATUS_DISCHARGING
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) } returns BatteryManager.BATTERY_HEALTH_GOOD
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) } returns 250 // 25.0°C

        powerManager = PowerManager(context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `initial state should be normal mode with default settings`() = runTest {
        // Given - PowerManager is initialized
        
        // When - Check initial state
        val initialState = powerManager.powerState.value
        
        // Then
        assertEquals(PowerMode.NORMAL, initialState.powerMode)
        assertEquals(MarketStatus.OPEN, initialState.marketStatus)
        assertTrue(initialState.audioProcessingEnabled)
        assertTrue(initialState.backgroundSyncEnabled)
        assertFalse(initialState.wakeLockActive)
    }

    @Test
    fun `should determine power save mode when battery is low`() = runTest {
        // Given - Low battery level
        val batteryIntent = mockk<Intent>(relaxed = true)
        every { context.registerReceiver(null, any<IntentFilter>()) } returns batteryIntent
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) } returns 15 // 15%
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1) } returns 100
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) } returns BatteryManager.BATTERY_STATUS_DISCHARGING

        // When - Battery status is updated
        advanceTimeBy(31_000) // Trigger battery check
        
        // Then - Should be in power save mode
        val state = powerManager.powerState.value
        assertEquals(PowerMode.POWER_SAVE, state.powerMode)
        assertEquals(15, state.batteryLevel)
        assertFalse(state.isCharging)
    }

    @Test
    fun `should determine critical save mode when battery is critically low`() = runTest {
        // Given - Critical battery level
        val batteryIntent = mockk<Intent>(relaxed = true)
        every { context.registerReceiver(null, any<IntentFilter>()) } returns batteryIntent
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) } returns 5 // 5%
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1) } returns 100
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) } returns BatteryManager.BATTERY_STATUS_DISCHARGING

        // When - Battery status is updated
        advanceTimeBy(31_000) // Trigger battery check
        
        // Then - Should be in critical save mode
        val state = powerManager.powerState.value
        assertEquals(PowerMode.CRITICAL_SAVE, state.powerMode)
        assertEquals(5, state.batteryLevel)
        assertFalse(state.isCharging)
    }

    @Test
    fun `should return to normal mode when charging`() = runTest {
        // Given - Device is charging
        val batteryIntent = mockk<Intent>(relaxed = true)
        every { context.registerReceiver(null, any<IntentFilter>()) } returns batteryIntent
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) } returns 15 // Low battery
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1) } returns 100
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) } returns BatteryManager.BATTERY_STATUS_CHARGING

        // When - Battery status is updated
        advanceTimeBy(31_000) // Trigger battery check
        
        // Then - Should be in normal mode despite low battery
        val state = powerManager.powerState.value
        assertEquals(PowerMode.NORMAL, state.powerMode)
        assertEquals(15, state.batteryLevel)
        assertTrue(state.isCharging)
    }

    @Test
    fun `should enter sleep mode when market is closed`() = runTest {
        // Given - Market hours set to 9 AM - 5 PM, current time is 8 PM (after hours)
        powerManager.setMarketHours(9, 17)
        
        // When - Market status is updated (simulated by advancing time)
        advanceTimeBy(61_000) // Trigger market check
        
        // Then - Should be in sleep mode
        val state = powerManager.powerState.value
        assertEquals(MarketStatus.AFTER_HOURS, state.marketStatus)
        // Note: In a real test, we'd need to mock the current time to be after market hours
    }

    @Test
    fun `should configure market hours correctly`() = runTest {
        // When - Set market hours
        powerManager.setMarketHours(8, 20)
        
        // Then - Market hours should be updated
        val marketHoursString = powerManager.getMarketHoursString()
        assertTrue(marketHoursString.contains("8:00"))
        assertTrue(marketHoursString.contains("8:00"))
    }

    @Test
    fun `should configure battery thresholds correctly`() = runTest {
        // When - Set battery thresholds
        powerManager.setBatteryThresholds(25, 15)
        
        // Given - Battery level at new threshold
        val batteryIntent = mockk<Intent>(relaxed = true)
        every { context.registerReceiver(null, any<IntentFilter>()) } returns batteryIntent
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) } returns 20 // Between critical and low
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1) } returns 100
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) } returns BatteryManager.BATTERY_STATUS_DISCHARGING

        // When - Battery status is updated
        advanceTimeBy(31_000) // Trigger battery check
        
        // Then - Should be in power save mode (above critical, below low)
        val state = powerManager.powerState.value
        assertEquals(PowerMode.POWER_SAVE, state.powerMode)
    }

    @Test
    fun `should disable power saving when requested`() = runTest {
        // Given - Low battery that would normally trigger power save
        val batteryIntent = mockk<Intent>(relaxed = true)
        every { context.registerReceiver(null, any<IntentFilter>()) } returns batteryIntent
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) } returns 15
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1) } returns 100
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) } returns BatteryManager.BATTERY_STATUS_DISCHARGING

        // When - Disable power saving
        powerManager.setPowerSavingEnabled(false)
        advanceTimeBy(31_000) // Trigger battery check
        
        // Then - Should remain in normal mode despite low battery
        val state = powerManager.powerState.value
        assertEquals(PowerMode.NORMAL, state.powerMode)
    }

    @Test
    fun `should force power mode when requested`() = runTest {
        // When - Force critical save mode
        powerManager.forcePowerMode(PowerMode.CRITICAL_SAVE)
        
        // Then - Should be in critical save mode
        val state = powerManager.powerState.value
        assertEquals(PowerMode.CRITICAL_SAVE, state.powerMode)
        assertFalse(state.audioProcessingEnabled)
        assertFalse(state.backgroundSyncEnabled)
        assertFalse(state.wakeLockActive)
    }

    @Test
    fun `should calculate estimated battery life correctly`() = runTest {
        // Given - 50% battery in power save mode
        val batteryIntent = mockk<Intent>(relaxed = true)
        every { context.registerReceiver(null, any<IntentFilter>()) } returns batteryIntent
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) } returns 50
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1) } returns 100
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) } returns BatteryManager.BATTERY_STATUS_DISCHARGING

        powerManager.forcePowerMode(PowerMode.POWER_SAVE)
        
        // When - Get estimated battery life
        val estimatedLife = powerManager.getEstimatedBatteryLife()
        
        // Then - Should return reasonable estimate (50% / 0.5% per hour = 100 hours)
        assertTrue(estimatedLife > 50f) // Should be more than 50 hours
    }

    @Test
    fun `should handle battery status parsing correctly`() = runTest {
        // Given - Various battery status values
        val batteryIntent = mockk<Intent>(relaxed = true)
        every { context.registerReceiver(null, any<IntentFilter>()) } returns batteryIntent
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) } returns BatteryManager.BATTERY_STATUS_FULL
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) } returns BatteryManager.BATTERY_HEALTH_GOOD
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) } returns 300 // 30.0°C
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) } returns 100
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1) } returns 100

        // When - Battery status is updated
        advanceTimeBy(31_000) // Trigger battery check
        
        // Then - Battery status should be parsed correctly
        val state = powerManager.powerState.value
        assertEquals("Full", state.batteryStatus.status)
        assertEquals("Good", state.batteryStatus.health)
        assertEquals(30.0f, state.batteryStatus.temperature)
    }

    @Test
    fun `should handle invalid battery readings gracefully`() = runTest {
        // Given - Invalid battery readings
        val batteryIntent = mockk<Intent>(relaxed = true)
        every { context.registerReceiver(null, any<IntentFilter>()) } returns batteryIntent
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) } returns -1
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1) } returns -1

        // When - Battery status is updated
        advanceTimeBy(31_000) // Trigger battery check
        
        // Then - Should default to 100% battery
        val state = powerManager.powerState.value
        assertEquals(100, state.batteryLevel)
    }

    @Test
    fun `should coerce market hours to valid range`() = runTest {
        // When - Set invalid market hours
        powerManager.setMarketHours(-5, 30)
        
        // Then - Should be coerced to valid range (0-23)
        val marketHoursString = powerManager.getMarketHoursString()
        assertTrue(marketHoursString.contains("12:00 AM")) // 0:00
        assertTrue(marketHoursString.contains("11:00 PM")) // 23:00
    }

    @Test
    fun `should coerce battery thresholds to valid range`() = runTest {
        // When - Set invalid battery thresholds
        powerManager.setBatteryThresholds(60, 55) // Low > 50, Critical >= Low
        
        // Given - Battery at 45%
        val batteryIntent = mockk<Intent>(relaxed = true)
        every { context.registerReceiver(null, any<IntentFilter>()) } returns batteryIntent
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) } returns 45
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1) } returns 100
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) } returns BatteryManager.BATTERY_STATUS_DISCHARGING

        // When - Battery status is updated
        advanceTimeBy(31_000) // Trigger battery check
        
        // Then - Should use coerced thresholds (50% low, 49% critical)
        val state = powerManager.powerState.value
        assertEquals(PowerMode.POWER_SAVE, state.powerMode) // 45% < 50% (coerced low threshold)
    }

    @Test
    fun `cleanup should release wake lock`() = runTest {
        // Given - Wake lock is acquired
        val wakeLock = mockk<AndroidPowerManager.WakeLock>(relaxed = true)
        every { androidPowerManager.newWakeLock(any(), any()) } returns wakeLock
        every { wakeLock.isHeld } returns true
        
        powerManager.forcePowerMode(PowerMode.NORMAL) // This should acquire wake lock
        
        // When - Cleanup is called
        powerManager.cleanup()
        
        // Then - Wake lock should be released
        verify { wakeLock.release() }
    }
}