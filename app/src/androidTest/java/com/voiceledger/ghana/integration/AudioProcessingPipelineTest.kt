package com.voiceledger.ghana.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.voiceledger.ghana.ml.audio.AudioUtils
import com.voiceledger.ghana.ml.entity.EntityExtractionService
import com.voiceledger.ghana.ml.speaker.SpeakerIdentificationService
import com.voiceledger.ghana.ml.speech.SpeechRecognitionManager
import com.voiceledger.ghana.ml.transaction.TransactionProcessor
import com.voiceledger.ghana.ml.vad.VADManager
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * End-to-end integration tests for audio processing pipeline
 * Tests the complete flow from audio input to transaction extraction
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AudioProcessingPipelineTest {
    
    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    @Inject
    lateinit var vadManager: VADManager
    
    @Inject
    lateinit var speechRecognitionManager: SpeechRecognitionManager
    
    @Inject
    lateinit var speakerIdentificationService: SpeakerIdentificationService
    
    @Inject
    lateinit var entityExtractionService: EntityExtractionService
    
    @Inject
    lateinit var transactionProcessor: TransactionProcessor
    
    @Inject
    lateinit var audioUtils: AudioUtils
    
    @Before
    fun setUp() {
        hiltRule.inject()
    }
    
    @Test
    fun testCompleteAudioProcessingPipeline_withValidTransaction_shouldExtractCorrectly() = runTest {
        // Given - mock audio data representing "I want 3 pieces of tilapia for 15 cedis"
        val mockAudioData = createMockAudioData()
        
        // When - process through complete pipeline
        val vadResult = vadManager.detectVoiceActivity(mockAudioData)
        assertTrue("Should detect voice activity", vadResult.hasVoice)
        
        val speechResult = speechRecognitionManager.recognizeSpeech(mockAudioData)
        assertNotNull("Should recognize speech", speechResult)
        
        val speakerResult = speakerIdentificationService.identifySpeaker(mockAudioData)
        assertNotNull("Should identify speaker", speakerResult)
        
        val entities = entityExtractionService.extractEntities(speechResult!!.text)
        assertNotNull("Should extract entities", entities)
        
        val transaction = transactionProcessor.processTransaction(
            speechResult.text, 
            speakerResult?.speakerId ?: "unknown"
        )
        
        // Then
        assertNotNull("Should create transaction", transaction)
        assertEquals("Should extract correct product", "tilapia", transaction?.productName)
        assertEquals("Should extract correct quantity", 3, transaction?.quantity)
        assertEquals("Should extract correct price", 15.0, transaction?.totalPrice, 0.01)
    }
    
    private fun createMockAudioData(): FloatArray {
        // Create mock audio data for testing
        return FloatArray(16000) { (Math.sin(2 * Math.PI * 440 * it / 16000) * 0.5).toFloat() }
    }
}