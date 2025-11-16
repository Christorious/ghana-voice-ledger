package com.voiceledger.ghana.service

import android.Manifest
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.voiceledger.ghana.service.AudioProcessingCallback
import com.voiceledger.ghana.service.ListeningState
import com.voiceledger.ghana.service.ServiceStats
import com.voiceledger.ghana.service.VoiceAgentService
import com.voiceledger.ghana.service.VoiceNotificationHelper
import com.voiceledger.ghana.service.VoiceSessionCoordinator
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class VoiceAgentServiceTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var testScope: TestScope

    private lateinit var service: VoiceAgentService
    private lateinit var mockSessionCoordinator: VoiceSessionCoordinator
    private lateinit var mockNotificationHelper: VoiceNotificationHelper

    private lateinit var listeningStateFlow: MutableStateFlow<ListeningState>

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        testScope = TestScope(testDispatcher)

        mockSessionCoordinator = mockk(relaxed = true)
        mockNotificationHelper = mockk(relaxed = true)

        listeningStateFlow = MutableStateFlow(ListeningState.STOPPED)

        every { mockSessionCoordinator.listeningState } returns listeningStateFlow
        coEvery { mockSessionCoordinator.initialize() } just Runs
        every { mockSessionCoordinator.startListening() } returns true
        every { mockSessionCoordinator.pauseListening() } just Runs
        every { mockSessionCoordinator.stopListening() } just Runs
        every { mockSessionCoordinator.cleanup() } just Runs
        every { mockSessionCoordinator.getServiceStats() } returns ServiceStats(
            totalChunksProcessed = 0,
            speechChunksDetected = 0,
            currentState = ListeningState.STOPPED,
            isInSleepMode = false,
            lastActivityTime = System.currentTimeMillis(),
            batteryLevel = 100
        )
        every { mockSessionCoordinator.getForegroundNotification() } returns mockk(relaxed = true)
        every { mockNotificationHelper.createNotificationChannel() } just Runs

        val context = ApplicationProvider.getApplicationContext<Context>()
        Shadows.shadowOf(context).grantPermissions(Manifest.permission.RECORD_AUDIO)

        val controller = Robolectric.buildService(VoiceAgentService::class.java)
        service = controller.get()

        setPrivateField(service, "serviceScope", testScope)
        setPrivateField(service, "sessionCoordinator", mockSessionCoordinator)
        setPrivateField(service, "notificationHelper", mockNotificationHelper)

        controller.create()
        testScope.runCurrent()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `onCreate initializes coordinator and notification channel`() {
        coVerify(exactly = 1) { mockSessionCoordinator.initialize() }
        verify(exactly = 1) { mockNotificationHelper.createNotificationChannel() }
    }

    @Test
    fun `startListening delegates to coordinator and starts foreground`() {
        service.startListening()
        testScope.runCurrent()

        coVerify { mockSessionCoordinator.startListening() }
        verify { mockSessionCoordinator.getForegroundNotification() }
    }

    @Test
    fun `pauseListening delegates to coordinator`() {
        service.pauseListening()

        verify { mockSessionCoordinator.pauseListening() }
    }

    @Test
    fun `stopListening delegates to coordinator`() {
        service.stopListening()

        verify { mockSessionCoordinator.stopListening() }
    }

    @Test
    fun `getServiceStats delegates to coordinator`() {
        service.getServiceStats()

        verify { mockSessionCoordinator.getServiceStats() }
    }

    @Test
    fun `onDestroy cleans up coordinator`() {
        service.onDestroy()
        testScope.runCurrent()

        verify { mockSessionCoordinator.cleanup() }
    }

    @Test
    fun `listeningState delegates to coordinator`() {
        val state = service.listeningState.value
        assertEquals(ListeningState.STOPPED, state)
        verify { mockSessionCoordinator.listeningState }
    }

    @Test
    fun `audioProcessingCallback delegates to coordinator`() {
        val mockCallback = mockk<AudioProcessingCallback>(relaxed = true)
        service.audioProcessingCallback = mockCallback

        verify { mockSessionCoordinator.setAudioProcessingCallback(mockCallback) }
    }

    private fun setPrivateField(target: Any, fieldName: String, value: Any) {
        val field = target::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        val existing = field.get(target)
        if (existing is CoroutineScope) {
            existing.cancel()
        }
        field.set(target, value)
    }
}