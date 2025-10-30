package com.voiceledger.ghana.service

import android.Manifest
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.test.core.app.ApplicationProvider
import com.voiceledger.ghana.domain.repository.AudioMetadataRepository
import com.voiceledger.ghana.ml.speaker.SpeakerIdentifier
import com.voiceledger.ghana.ml.transaction.TransactionProcessor
import com.voiceledger.ghana.ml.vad.SleepMode
import com.voiceledger.ghana.ml.vad.VADManager
import com.voiceledger.ghana.ml.vad.VADResult
import com.voiceledger.ghana.ml.vad.VADType
import com.voiceledger.ghana.offline.NetworkState
import com.voiceledger.ghana.offline.NetworkUtils
import com.voiceledger.ghana.offline.OfflineQueueManager
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
import org.robolectric.shadows.ShadowSystemClock
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class VoiceAgentServiceTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var testScope: TestScope

    private lateinit var service: VoiceAgentService

    private lateinit var audioMetadataRepository: AudioMetadataRepository
    private lateinit var vadManager: VADManager
    private lateinit var speakerIdentifier: SpeakerIdentifier
    private lateinit var transactionProcessor: TransactionProcessor
    private lateinit var speechRecognitionManager: com.voiceledger.ghana.ml.speech.SpeechRecognitionManager
    private lateinit var powerManager: PowerManager
    private lateinit var offlineQueueManager: OfflineQueueManager

    private lateinit var sleepModeFlow: MutableStateFlow<SleepMode>
    private lateinit var powerStateFlow: MutableStateFlow<PowerState>
    private lateinit var networkStateFlow: MutableStateFlow<NetworkState>

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        testScope = TestScope(testDispatcher)

        mockkObject(NetworkUtils)

        audioMetadataRepository = mockk(relaxed = true)
        vadManager = mockk(relaxed = true)
        speakerIdentifier = mockk(relaxed = true)
        transactionProcessor = mockk(relaxed = true)
        speechRecognitionManager = mockk(relaxed = true)
        powerManager = mockk(relaxed = true)
        offlineQueueManager = mockk(relaxed = true)

        sleepModeFlow = MutableStateFlow(SleepMode.AWAKE)
        powerStateFlow = MutableStateFlow(PowerState())
        networkStateFlow = MutableStateFlow(NetworkState())

        coEvery { vadManager.initialize(VADType.CUSTOM) } returns true
        coEvery { vadManager.startProcessing() } returns Unit
        coEvery { vadManager.stopProcessing() } returns Unit
        coEvery { vadManager.destroy() } returns Unit
        every { vadManager.sleepModeChanges } returns sleepModeFlow
        coEvery { vadManager.processAudioSample(any()) } returns VADResult(false, 0f, 0f)
        every { vadManager.shouldUsePowerSavingMode() } returns false

        coEvery { speakerIdentifier.initialize() } returns Unit
        every { speakerIdentifier.cleanup() } returns Unit

        coEvery { speechRecognitionManager.optimizeForMarketEnvironment() } returns Unit
        every { speechRecognitionManager.cleanup() } returns Unit
        every { speechRecognitionManager.setPreferOnlineRecognition(any()) } returns Unit

        coEvery { transactionProcessor.cleanup() } returns Unit

        every { powerManager.powerState } returns powerStateFlow
        every { powerManager.cleanup() } returns Unit

        coEvery { offlineQueueManager.processAllPendingOperations() } returns Unit
        every { offlineQueueManager.cleanup() } returns Unit

        every { NetworkUtils.initialize(any()) } returns Unit
        every { NetworkUtils.cleanup() } returns Unit
        every { NetworkUtils.networkState } returns networkStateFlow

        val context = ApplicationProvider.getApplicationContext<Context>()
        Shadows.shadowOf(context).grantPermissions(Manifest.permission.RECORD_AUDIO)

        val controller = Robolectric.buildService(VoiceAgentService::class.java)
        service = controller.get()

        setPrivateField(service, "serviceScope", testScope)

        service.audioMetadataRepository = audioMetadataRepository
        service.vadManager = vadManager
        service.speakerIdentifier = speakerIdentifier
        service.transactionProcessor = transactionProcessor
        service.speechRecognitionManager = speechRecognitionManager
        service.powerManager = powerManager
        service.offlineQueueManager = offlineQueueManager

        controller.create()
        testScope.runCurrent()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `onCreate initializes coordinator components`() {
        coVerify(exactly = 1) { vadManager.initialize(VADType.CUSTOM) }
        coVerify(exactly = 1) { speakerIdentifier.initialize() }
        coVerify(exactly = 1) { speechRecognitionManager.optimizeForMarketEnvironment() }
        verify(exactly = 1) { NetworkUtils.initialize(service) }
    }

    @Test
    fun `startListening without permission sets error state`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        Shadows.shadowOf(context).denyPermissions(Manifest.permission.RECORD_AUDIO)

        service.startListening()

        val state = service.listeningState.value
        assertIs<ListeningState.ERROR>(state)
        coVerify(exactly = 0) { vadManager.startProcessing() }
    }

    @Test
    fun `startListening with permission enters listening state and starts VAD`() {
        ShadowSystemClock.setCurrentTimeMillis(TimeUnit.HOURS.toMillis(9))

        mockkStatic(AudioRecord::class)
        every {
            AudioRecord.getMinBufferSize(
                VoiceAgentServiceTestUtils.SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
        } returns 1024

        mockkConstructor(AudioRecord::class)
        every { anyConstructed<AudioRecord>().state } returns AudioRecord.STATE_INITIALIZED
        every { anyConstructed<AudioRecord>().startRecording() } returns Unit
        every { anyConstructed<AudioRecord>().read(any(), any(), any()) } returns 0
        every { anyConstructed<AudioRecord>().stop() } returns Unit
        every { anyConstructed<AudioRecord>().release() } returns Unit

        try {
            service.startListening()
            testScope.runCurrent()

            assertEquals(ListeningState.LISTENING, service.listeningState.value)
            coVerify { vadManager.startProcessing() }
        } finally {
            service.stopListening()
            unmockkConstructor(AudioRecord::class)
            unmockkStatic(AudioRecord::class)
        }
    }

    @Test
    fun `network availability triggers offline queue processing`() {
        networkStateFlow.value = NetworkState(isAvailable = true)
        testScope.runCurrent()

        coVerify { offlineQueueManager.processAllPendingOperations() }
        verify { speechRecognitionManager.setPreferOnlineRecognition(true) }
    }

    @Test
    fun `sleep mode update transitions service state`() {
        sleepModeFlow.value = SleepMode.LIGHT_SLEEP
        testScope.runCurrent()

        assertEquals(ListeningState.SLEEPING, service.listeningState.value)
    }

    @Test
    fun `onDestroy cleans up dependencies`() {
        service.onDestroy()
        testScope.runCurrent()

        coVerify { vadManager.stopProcessing() }
        coVerify { vadManager.destroy() }
        verify { speakerIdentifier.cleanup() }
        verify { speechRecognitionManager.cleanup() }
        coVerify { transactionProcessor.cleanup() }
        verify { powerManager.cleanup() }
        verify { offlineQueueManager.cleanup() }
        verify { NetworkUtils.cleanup() }
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

    private object VoiceAgentServiceTestUtils {
        const val SAMPLE_RATE = 16000
    }
}
