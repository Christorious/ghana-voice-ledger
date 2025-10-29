package com.voiceledger.ghana.service

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class VoiceAgentServiceTest {
    
    private lateinit var service: VoiceAgentService
    private lateinit var mockCoordinator: VoiceSessionCoordinator
    private lateinit var mockNotificationHelper: VoiceNotificationHelper
    
    private val testDispatcher = UnconfinedTestDispatcher()
    
    @Before
    fun setUp() {
        mockCoordinator = mockk(relaxed = true)
        mockNotificationHelper = mockk(relaxed = true)
        
        val stateFlow = MutableStateFlow<ListeningState>(ListeningState.STOPPED)
        every { mockCoordinator.listeningState } returns stateFlow
        coEvery { mockCoordinator.initialize() } just Runs
        every { mockCoordinator.startListening() } returns true
        every { mockCoordinator.getForegroundNotification() } returns mockk(relaxed = true)
        every { mockNotificationHelper.createNotificationChannel() } just Runs
        
        service = VoiceAgentService()
        service.sessionCoordinator = mockCoordinator
        service.notificationHelper = mockNotificationHelper
    }
    
    @After
    fun tearDown() {
        clearAllMocks()
    }
    
    @Test
    fun testOnCreate_shouldInitializeComponents() {
        service.onCreate()
        
        verify { mockNotificationHelper.createNotificationChannel() }
    }
    
    @Test
    fun testGetServiceStats_shouldDelegateToCoordinator() {
        val mockStats = ServiceStats(
            totalChunksProcessed = 100,
            speechChunksDetected = 50,
            currentState = ListeningState.LISTENING,
            isInSleepMode = false,
            lastActivityTime = System.currentTimeMillis(),
            batteryLevel = 80
        )
        every { mockCoordinator.getServiceStats() } returns mockStats
        
        val stats = service.getServiceStats()
        
        assertNotNull(stats)
        verify { mockCoordinator.getServiceStats() }
    }
    
    @Test
    fun testPauseListening_shouldDelegateToCoordinator() {
        service.pauseListening()
        
        verify { mockCoordinator.pauseListening() }
    }
    
    @Test
    fun testStopListening_shouldDelegateToCoordinator() {
        service.stopListening()
        
        verify { mockCoordinator.stopListening() }
    }
}
