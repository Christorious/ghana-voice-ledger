package com.voiceledger.ghana.integration

import android.content.Context
import android.os.BatteryManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.voiceledger.ghana.performance.PerformanceMonitor
import com.voiceledger.ghana.service.PowerManager
import com.voiceledger.ghana.service.VoiceAgentService
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Integration tests for battery usage validation over extended periods
 * Tests power optimization and battery consumption patterns
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class BatteryUsageIntegrationTest {
    
    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    @Inject
    lateinit var powerManager: PowerManager
    
    @Inject
    lateinit var performanceMonitor: PerformanceMonitor
    
    private lateinit var context: Context
    private lateinit var batteryManager: BatteryManager
    
    @Before
    fun setUp() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
        batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    }
    
    @Test
    fun testBatteryUsageWithContinuousListening_shouldOptimizePowerConsumption() = runTest {
        // Given - record initial battery state
        val initialBatteryLevel = getCurrentBatteryLevel()
        val initialBatteryTemp = getCurrentBatteryTemperature()
        
        // Start performance monitoring
        performanceMonitor.startMonitoring()
        
        // When - simulate continuous listening for extended period
        powerManager.enablePowerSavingMode()
        
        // Simulate voice agent running for 30 seconds (scaled down for testing)
        repeat(30) {
            // Simulate voice processing workload
            simulateVoiceProcessingWorkload()
            delay(1000) // 1 second intervals
        }
        
        // Then - check battery consumption
        val finalBatteryLevel = getCurrentBatteryLevel()
        val finalBatteryTemp = getCurrentBatteryTemperature()
        val batteryDrain = initialBatteryLevel - finalBatteryLevel
        
        // Stop monitoring and get metrics
        val metrics = performanceMonitor.stopMonitoring()
        
        // Validate reasonable battery consumption
        assertTrue("Battery drain should be minimal during power saving", batteryDrain <= 2)
        assertTrue("Battery temperature should not increase significantly", 
            finalBatteryTemp - initialBatteryTemp <= 5) // 5°C tolerance
        
        // Validate performance metrics
        assertNotNull("Should have performance metrics", metrics)
        assertTrue("CPU usage should be optimized", metrics.averageCpuUsage < 50.0)
        assertTrue("Memory usage should be controlled", metrics.averageMemoryUsage < 200 * 1024 * 1024) // 200MB
    }
    
    @Test
    fun testBatteryOptimizationModes_shouldAdjustPerformanceBasedOnBatteryLevel() = runTest {
        // Given - different battery levels
        val batteryLevels = listOf(90, 50, 20, 10) // High, Medium, Low, Critical
        
        batteryLevels.forEach { batteryLevel ->
            // When - simulate battery level and apply optimization
            simulateBatteryLevel(batteryLevel)
            powerManager.optimizeForBatteryLevel(batteryLevel)
            
            // Then - verify appropriate optimization mode
            val currentMode = powerManager.getCurrentPowerMode()
            
            when (batteryLevel) {
                in 80..100 -> assertEquals("Should use performance mode for high battery", 
                    PowerManager.PowerMode.PERFORMANCE, currentMode)
                in 50..79 -> assertEquals("Should use balanced mode for medium battery", 
                    PowerManager.PowerMode.BALANCED, currentMode)
                in 20..49 -> assertEquals("Should use power saver mode for low battery", 
                    PowerManager.PowerMode.POWER_SAVER, currentMode)
                else -> assertEquals("Should use ultra power saver for critical battery", 
                    PowerManager.PowerMode.ULTRA_POWER_SAVER, currentMode)
            }
        }
    }
    
    @Test
    fun testBackgroundProcessingOptimization_shouldReducePowerWhenInactive() = runTest {
        // Given - start with active processing
        performanceMonitor.startMonitoring()
        powerManager.setActiveProcessing(true)
        
        // Simulate active processing for 10 seconds
        repeat(10) {
            simulateVoiceProcessingWorkload()
            delay(1000)
        }
        
        val activeMetrics = performanceMonitor.getCurrentMetrics()
        
        // When - switch to background/inactive mode
        powerManager.setActiveProcessing(false)
        
        // Simulate background processing for 10 seconds
        repeat(10) {
            simulateBackgroundWorkload()
            delay(1000)
        }
        
        val backgroundMetrics = performanceMonitor.getCurrentMetrics()
        performanceMonitor.stopMonitoring()
        
        // Then - verify reduced power consumption in background
        assertTrue("CPU usage should be lower in background", 
            backgroundMetrics.averageCpuUsage < activeMetrics.averageCpuUsage)
        assertTrue("Memory usage should be optimized in background", 
            backgroundMetrics.averageMemoryUsage <= activeMetrics.averageMemoryUsage)
    }
    
    @Test
    fun testThermalThrottling_shouldReducePerformanceWhenOverheating() = runTest {
        // Given - simulate high temperature scenario
        val initialTemp = getCurrentBatteryTemperature()
        
        // When - simulate intensive processing that might cause heating
        performanceMonitor.startMonitoring()
        
        repeat(20) {
            simulateIntensiveProcessingWorkload()
            delay(500)
            
            // Check if thermal throttling should kick in
            val currentTemp = getCurrentBatteryTemperature()
            if (currentTemp > initialTemp + 10) { // 10°C increase threshold
                powerManager.enableThermalThrottling()
                break
            }
        }
        
        val metrics = performanceMonitor.stopMonitoring()
        
        // Then - verify thermal management
        val finalTemp = getCurrentBatteryTemperature()
        assertTrue("Temperature increase should be controlled", 
            finalTemp - initialTemp <= 15) // 15°C max increase
        
        if (powerManager.isThermalThrottlingEnabled()) {
            assertTrue("CPU usage should be throttled when overheating", 
                metrics.averageCpuUsage < 70.0)
        }
    }
    
    @Test
    fun testLongRunningServiceBatteryImpact_shouldMaintainEfficiency() = runTest {
        // Given - start long-running voice service
        val initialBatteryLevel = getCurrentBatteryLevel()
        performanceMonitor.startMonitoring()
        
        // When - simulate service running for extended period (2 minutes scaled)
        val serviceStartTime = System.currentTimeMillis()
        
        repeat(120) { // 2 minutes
            // Simulate periodic voice processing
            if (it % 10 == 0) { // Every 10 seconds
                simulateVoiceProcessingWorkload()
            } else {
                simulateIdleServiceWorkload()
            }
            delay(1000)
        }
        
        val serviceRunTime = System.currentTimeMillis() - serviceStartTime
        val finalBatteryLevel = getCurrentBatteryLevel()
        val metrics = performanceMonitor.stopMonitoring()
        
        // Then - validate service efficiency
        val batteryDrainRate = (initialBatteryLevel - finalBatteryLevel) / (serviceRunTime / 60000.0) // per minute
        
        assertTrue("Battery drain rate should be acceptable for long-running service", 
            batteryDrainRate <= 1.0) // Max 1% per minute
        assertTrue("Memory usage should remain stable", 
            metrics.maxMemoryUsage < 300 * 1024 * 1024) // 300MB max
        assertTrue("CPU usage should be efficient", 
            metrics.averageCpuUsage < 30.0) // 30% average
    }
    
    // Helper methods
    private fun getCurrentBatteryLevel(): Int {
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }
    
    private fun getCurrentBatteryTemperature(): Int {
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
    }
    
    private fun simulateBatteryLevel(level: Int) {
        // In a real test, this would mock the battery level
        // For now, we'll just notify the power manager
        powerManager.onBatteryLevelChanged(level)
    }
    
    private suspend fun simulateVoiceProcessingWorkload() {
        // Simulate CPU-intensive voice processing
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < 100) {
            // Simulate processing work
            Math.sin(Math.random() * 1000)
        }
    }
    
    private suspend fun simulateBackgroundWorkload() {
        // Simulate lighter background processing
        delay(50)
        Math.sin(Math.random() * 100)
    }
    
    private suspend fun simulateIntensiveProcessingWorkload() {
        // Simulate very intensive processing
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < 200) {
            // More intensive work
            for (i in 0..1000) {
                Math.sin(Math.random() * i)
            }
        }
    }
    
    private suspend fun simulateIdleServiceWorkload() {
        // Simulate minimal idle processing
        delay(100)
    }
}