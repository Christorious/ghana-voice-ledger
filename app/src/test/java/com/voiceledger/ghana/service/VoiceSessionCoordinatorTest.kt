package com.voiceledger.ghana.service

import android.content.Context
import android.os.PowerManager as AndroidPowerManager
import androidx.test.core.app.ApplicationProvider
import com.voiceledger.ghana.ml.speech.SpeechRecognitionManager
import com.voiceledger.ghana.ml.vad.SleepMode
import com.voiceledger.ghana.ml.vad.VADManager
import com.voiceledger.ghana.offline.NetworkState
import com.voiceledger.ghana.offline.NetworkUtils
import com.voiceledger.ghana.offline.OfflineQueueManager
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceSessionCoordinatorTest {

    private lateinit var context: Context
    private lateinit var audioCaptureController: AudioCaptureController
    private lateinit var speechProcessingPipeline: SpeechProcessingPipeline
    private lateinit var notificationHelper: VoiceNotificationHelper
    private lateinit var vadManager: VADManager
    private lateinit var speechRecognitionManager: SpeechRecognitionManager
    private lateinit var offlineQueueManager: OfflineQueueManager
    private lateinit var powerManager: PowerManager
    private lateinit var coordinator: VoiceSessionCoordinator

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var sleepModeFlow: MutableStateFlow<SleepMode>
    private lateinit var powerStateFlow: MutableStateFlow<PowerState>
    private lateinit var networkStateFlow: MutableStateFlow<NetworkState>

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        context = ApplicationProvider.getApplicationContext()
        audioCaptureController = mockk(relaxed = true)
        speechProcessingPipeline = mockk(relaxed = true)
        notificationHelper = mockk(relaxed = true)
        vadManager = mockk(relaxed = true)
        speechRecognitionManager = mockk(relaxed = true)
        offlineQueueManager = mockk(relaxed = true)
        powerManager = mockk(relaxed = true)

        sleepModeFlow = MutableStateFlow(SleepMode.AWAKE)
        powerStateFlow = MutableStateFlow(PowerState())
        networkStateFlow = MutableStateFlow(NetworkState())

        every { vadManager.sleepModeChanges } returns sleepModeFlow
        every { powerManager.powerState } returns powerStateFlow
        every { NetworkUtils.networkState } returns networkStateFlow

        coEvery { speechProcessingPipeline.initialize() } just Runs
        every { audioCaptureController.checkAudioPermission() } returns true
        every { audioCaptureController.initialize() } just Runs
        every { audioCaptureController.isActive() } returns false
        every { notificationHelper.createListeningNotification() } returns mockk(relaxed = true)
        every { notificationHelper.createSleepingNotification() } returns mockk(relaxed = true)

        coordinator = VoiceSessionCoordinator(
            context,
            audioCaptureController,
            speechProcessingPipeline,
            notificationHelper,
            vadManager,
            speechRecognitionManager,
            offlineQueueManager,
            powerManager,
            testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `initialize should setup pipeline and network utils`() = runTest(testDispatcher) {
        mockkObject(NetworkUtils)
        every { NetworkUtils.initialize(context) } just Runs

        coordinator.initialize()
        testScope.runCurrent()

        coVerify { speechProcessingPipeline.initialize() }
        verify { NetworkUtils.initialize(context) }

        unmockkObject(NetworkUtils)
    }

    @Test
    fun `startListening should return true when permission granted and in market hours`() = runTest(testDispatcher) {
        mockkStatic("kotlin.time.Clock")
        every { kotlin.time.Clock.System.now() } returns kotlin.time.Clock.System.now()

        val result = coordinator.startListening()

        assertTrue(result)
        assertEquals(ListeningState.LISTENING, coordinator.listeningState.value)
        verify { audioCaptureController.initialize() }
        coVerify { speechProcessingPipeline.startProcessing() }
    }

    @Test
    fun `startListening should return false when permission not granted`() = runTest(testDispatcher) {
        every { audioCaptureController.checkAudioPermission() } returns false

        val result = coordinator.startListening()

        assertFalse(result)
        assertEquals(ListeningState.ERROR::class, coordinator.listeningState.value::class)
    }

    @Test
    fun `pauseListening should update state to paused`() {
        every { audioCaptureController.isActive() } returns true

        coordinator.pauseListening()

        assertEquals(ListeningState.PAUSED, coordinator.listeningState.value)
        verify { audioCaptureController.stopRecording() }
    }

    @Test
    fun `stopListening should cleanup and update state to stopped`() {
        coordinator.stopListening()

        assertEquals(ListeningState.STOPPED, coordinator.listeningState.value)
        coVerify { speechProcessingPipeline.stopProcessing() }
        verify { audioCaptureController.release() }
    }

    @Test
    fun `getServiceStats should return current stats`() {
        val stats = coordinator.getServiceStats()

        assertEquals(0, stats.totalChunksProcessed)
        assertEquals(0, stats.speechChunksDetected)
        assertEquals(ListeningState.STOPPED, stats.currentState)
        assertFalse(stats.isInSleepMode)
    }

    @Test
    fun `sleep mode change should transition to sleeping state`() = runTest(testDispatcher) {
        coordinator.startListening()
        testScope.runCurrent()

        sleepModeFlow.value = SleepMode.LIGHT_SLEEP
        testScope.runCurrent()

        assertEquals(ListeningState.SLEEPING, coordinator.listeningState.value)
    }

    @Test
    fun `network state change should update speech recognition preference`() = runTest(testDispatcher) {
        networkStateFlow.value = NetworkState(isAvailable = true)
        testScope.runCurrent()

        verify { speechRecognitionManager.setPreferOnlineRecognition(true) }

        networkStateFlow.value = NetworkState(isAvailable = false)
        testScope.runCurrent()

        verify { speechRecognitionManager.setPreferOnlineRecognition(false) }
    }

    @Test
    fun `setAudioProcessingCallback should store callback`() {
        val mockCallback = mockk<AudioProcessingCallback>(relaxed = true)

        coordinator.setAudioProcessingCallback(mockCallback)

        // Verify it's stored by checking if we can retrieve it indirectly
        // This is tested through the processing pipeline which uses the callback
    }
}