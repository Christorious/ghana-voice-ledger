package com.voiceledger.ghana.service

import com.voiceledger.ghana.data.local.entity.AudioMetadata
import com.voiceledger.ghana.domain.repository.AudioMetadataRepository
import com.voiceledger.ghana.ml.speaker.SpeakerIdentifier
import com.voiceledger.ghana.ml.speaker.SpeakerResult
import com.voiceledger.ghana.ml.speaker.SpeakerType
import com.voiceledger.ghana.ml.speech.SpeechRecognitionManager
import com.voiceledger.ghana.ml.speech.TranscriptionResult
import com.voiceledger.ghana.ml.transaction.TransactionProcessor
import com.voiceledger.ghana.ml.vad.VADManager
import com.voiceledger.ghana.ml.vad.VADResult
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SpeechProcessingPipelineTest {
    
    private lateinit var vadManager: VADManager
    private lateinit var speakerIdentifier: SpeakerIdentifier
    private lateinit var speechRecognitionManager: SpeechRecognitionManager
    private lateinit var transactionProcessor: TransactionProcessor
    private lateinit var audioMetadataRepository: AudioMetadataRepository
    private lateinit var pipeline: SpeechProcessingPipeline
    
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        vadManager = mockk(relaxed = true)
        speakerIdentifier = mockk(relaxed = true)
        speechRecognitionManager = mockk(relaxed = true)
        transactionProcessor = mockk(relaxed = true)
        audioMetadataRepository = mockk(relaxed = true)
        
        coEvery { vadManager.initialize(any()) } just Runs
        coEvery { speakerIdentifier.initialize() } just Runs
        coEvery { speechRecognitionManager.optimizeForMarketEnvironment() } just Runs
        coEvery { audioMetadataRepository.insertMetadata(any()) } just Runs
        
        pipeline = SpeechProcessingPipeline(
            vadManager,
            speakerIdentifier,
            speechRecognitionManager,
            transactionProcessor,
            audioMetadataRepository,
            testDispatcher,
            testDispatcher
        )
    }
    
    @After
    fun tearDown() {
        pipeline.cleanup()
        Dispatchers.resetMain()
        clearAllMocks()
    }
    
    @Test
    fun testInitialize_shouldInitializeAllComponents() = runTest(testDispatcher) {
        pipeline.initialize()
        
        coVerify { vadManager.initialize(any()) }
        coVerify { speakerIdentifier.initialize() }
        coVerify { speechRecognitionManager.optimizeForMarketEnvironment() }
    }
    
    @Test
    fun testSubmitChunk_withNoSpeech_shouldStoreMetadata() = runTest(testDispatcher) {
        val vadResult = VADResult(isSpeech = false, confidence = 0.1f, energyLevel = 0.05f)
        coEvery { vadManager.processAudioSample(any()) } returns vadResult
        
        val audioChunk = AudioChunk(
            data = ShortArray(16000),
            samplesRead = 16000,
            timestamp = System.currentTimeMillis()
        )
        
        pipeline.submitChunk(audioChunk, 80, false, null)
        
        advanceUntilIdle()
        
        coVerify { vadManager.processAudioSample(any()) }
        coVerify { audioMetadataRepository.insertMetadata(any()) }
        coVerify(exactly = 0) { speakerIdentifier.identifySpeaker(any()) }
    }
    
    @Test
    fun testSubmitChunk_withSpeech_shouldProcessSpeaker() = runTest(testDispatcher) {
        val vadResult = VADResult(isSpeech = true, confidence = 0.8f, energyLevel = 0.5f)
        val speakerResult = SpeakerResult(
            speakerType = SpeakerType.SELLER,
            speakerId = "seller_1",
            confidence = 0.9f,
            audioFeatures = emptyList()
        )
        val transcriptionResult = TranscriptionResult(
            transcript = "Test transcript",
            confidence = 0.85f,
            isSuccess = true,
            language = "en",
            duration = 1000L
        )
        
        coEvery { vadManager.processAudioSample(any()) } returns vadResult
        coEvery { speakerIdentifier.identifySpeaker(any()) } returns speakerResult
        coEvery { speechRecognitionManager.transcribe(any()) } returns transcriptionResult
        coEvery { transactionProcessor.processUtterance(any(), any(), any(), any(), any(), any()) } just Runs
        
        val audioChunk = AudioChunk(
            data = ShortArray(16000),
            samplesRead = 16000,
            timestamp = System.currentTimeMillis()
        )
        
        pipeline.submitChunk(audioChunk, 80, false, null)
        
        advanceUntilIdle()
        
        coVerify { vadManager.processAudioSample(any()) }
        coVerify { speakerIdentifier.identifySpeaker(any()) }
        coVerify { speechRecognitionManager.transcribe(any()) }
        coVerify { transactionProcessor.processUtterance(any(), any(), any(), any(), any(), any()) }
        coVerify { audioMetadataRepository.insertMetadata(any()) }
    }
    
    @Test
    fun testStartProcessing_shouldStartVAD() {
        every { vadManager.startProcessing() } just Runs
        
        pipeline.startProcessing()
        
        verify { vadManager.startProcessing() }
    }
    
    @Test
    fun testStopProcessing_shouldStopVAD() {
        every { vadManager.stopProcessing() } just Runs
        
        pipeline.stopProcessing()
        
        verify { vadManager.stopProcessing() }
    }
    
    @Test
    fun testCleanup_shouldCleanupAllComponents() {
        every { vadManager.destroy() } just Runs
        every { speakerIdentifier.cleanup() } just Runs
        every { speechRecognitionManager.cleanup() } just Runs
        every { transactionProcessor.cleanup() } just Runs
        
        pipeline.cleanup()
        
        verify { vadManager.destroy() }
        verify { speakerIdentifier.cleanup() }
        verify { speechRecognitionManager.cleanup() }
        verify { transactionProcessor.cleanup() }
    }
}
