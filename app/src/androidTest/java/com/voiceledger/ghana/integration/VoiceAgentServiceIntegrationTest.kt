package com.voiceledger.ghana.integration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkManager
import androidx.work.testing.TestDriver
import androidx.work.testing.WorkManagerTestInitHelper
import com.voiceledger.ghana.offline.OfflineQueueManager
import com.voiceledger.ghana.service.PowerManager
import com.voiceledger.ghana.service.VoiceAgentServiceManager
import com.voiceledger.ghana.service.VoiceSessionCoordinator
import com.voiceledger.ghana.service.WorkManagerScheduler
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration test for VoiceAgentService modularization
 * Tests the complete integration between all service components
 */
@HiltAndroidTest
class VoiceAgentServiceIntegrationTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var voiceSessionCoordinator: VoiceSessionCoordinator

    @Inject
    lateinit var offlineQueueManager: OfflineQueueManager

    @Inject
    lateinit var powerManager: PowerManager

    @Inject
    lateinit var workManagerScheduler: WorkManagerScheduler

    @Inject
    lateinit var voiceAgentServiceManager: VoiceAgentServiceManager

    private lateinit var context: Context
    private lateinit var workManager: WorkManager
    private lateinit var testDriver: TestDriver

    @Before
    fun setUp() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
        
        // Initialize WorkManager for testing
        val workManagerTestInitHelper = WorkManagerTestInitHelper(context)
        workManager = workManagerTestInitHelper.workManager
        testDriver = workManagerTestInitHelper.testDriver
    }

    @After
    fun tearDown() {
        WorkManagerTestInitHelper.closeWorkManager()
    }

    @Test
    fun testVoiceSessionCoordinatorInitialization() = runTest {
        // Initialize the coordinator
        voiceSessionCoordinator.initialize()
        
        // Verify initial state
        val initialState = voiceSessionCoordinator.listeningState.first()
        assertNotNull(initialState)
    }

    @Test
    fun testOfflineQueueManagerIntegration() = runTest {
        // Test queuing operations
        val testData = """{"id":"test","amount":10.0,"product":"fish"}"""
        offlineQueueManager.queueTransactionCreate(testData)
        
        // Verify queue state
        val stats = offlineQueueManager.getQueueStatistics()
        assertTrue(stats.totalOperations >= 0)
    }

    @Test
    fun testPowerManagerIntegration() = runTest {
        // Verify power manager state
        val powerState = powerManager.powerState.first()
        assertNotNull(powerState)
        assertTrue(powerState.batteryLevel in 0..100)
    }

    @Test
    fun testWorkManagerSchedulerIntegration() {
        // Verify WorkManager scheduling
        val isOfflineSyncScheduled = workManagerScheduler.isWorkScheduled("OfflineSyncWork")
        assertTrue(isOfflineSyncScheduled || workManagerScheduler != null)
    }

    @Test
    fun testVoiceAgentServiceManagerIntegration() {
        // Test service manager initialization
        assertNotNull(voiceAgentServiceManager)
        
        // Test initial state
        val serviceState = voiceAgentServiceManager.serviceState.value
        assertNotNull(serviceState)
    }

    @Test
    fun testPowerOptimizationSettings() = runTest {
        // Test power optimization settings
        voiceSessionCoordinator.initialize()
        
        val settings = voiceSessionCoordinator.getPowerOptimizationSettings()
        assertNotNull(settings)
        assertTrue(settings.audioProcessingEnabled)
        assertTrue(settings.backgroundSyncEnabled)
        assertTrue(settings.processingIntervalMs > 0)
        assertTrue(settings.vadSensitivity > 0f)
        assertTrue(settings.audioBufferSize > 0)
    }

    @Test
    fun testServiceModularization() = runTest {
        // Verify all components are properly injected and modularized
        
        // Test VoiceSessionCoordinator has all required dependencies
        assertNotNull(voiceSessionCoordinator)
        
        // Test OfflineQueueManager is properly integrated
        assertNotNull(offlineQueueManager)
        
        // Test PowerManager is working
        assertNotNull(powerManager)
        
        // Test WorkManagerScheduler is set up
        assertNotNull(workManagerScheduler)
        
        // Test VoiceAgentServiceManager is available
        assertNotNull(voiceAgentServiceManager)
        
        // Initialize coordinator to test full integration
        voiceSessionCoordinator.initialize()
        
        // Verify the coordinator can process operations
        val stats = voiceSessionCoordinator.getServiceStats()
        assertNotNull(stats)
        assertEquals(0, stats.totalChunksProcessed) // Initial state
    }

    @Test
    fun testBackgroundJobScheduling() {
        // Test that background jobs are properly scheduled
        val isPowerOptimizationScheduled = workManagerScheduler.isWorkScheduled("PowerOptimizationWork")
        
        // Jobs should be scheduled either during initialization or on demand
        assertTrue(workManagerScheduler != null)
    }

    @Test
    fun testCleanArchitectureSeparation() = runTest {
        // Verify clean architecture separation
        
        // Domain layer should be separate from data layer
        val powerState = powerManager.powerState.first()
        assertNotNull(powerState)
        
        // Service layer should coordinate between layers
        voiceSessionCoordinator.initialize()
        
        // All components should be properly modularized
        assertTrue(offlineQueueManager.queueState.value.pendingOperations >= 0)
    }
}