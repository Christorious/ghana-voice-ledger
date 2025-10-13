package com.voiceledger.ghana.ml.speaker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer

/**
 * Unit tests for TensorFlowLiteSpeakerIdentifier with mocked TensorFlow Lite models
 */
@RunWith(AndroidJUnit4::class)
class TensorFlowLiteSpeakerIdentifierTest {
    
    private lateinit var context: Context
    @Mock
    private lateinit var mockInterpreter: Interpreter
    @Mock
    private lateinit var mockModelLoader: ModelLoader
    
    private lateinit var speakerIdentifier: TensorFlowLiteSpeakerIdentifier
    
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        
        // Mock model loading
        whenever(mockModelLoader.loadModel(any())).thenReturn(mockInterpreter)
        
        speakerIdentifier = TensorFlowLiteSpeakerIdentifier(context, mockModelLoader)
    }
    
    @Test
    fun testInitialize_shouldLoadModelSuccessfully() = runTest {
        // When
        val result = speakerIdentifier.initialize()
        
        // Then
        assertTrue("Initialization should succeed", result)
        verify(mockModelLoader).loadModel(any())
    }
    
    @Test
    fun testInitialize_withModelLoadFailure_shouldReturnFalse() = runTest {
        // Given
        whenever(mockModelLoader.loadModel(any())).thenThrow(RuntimeException("Model load failed"))
        
        // When
        val result = speakerIdentifier.initialize()
        
        // Then
        assertFalse("Initialization should fail", result)
    }
    
    @Test
    fun testIdentifySpeaker_withKnownSpeaker_shouldReturnCorrectId() = runTest {
        // Given
        speakerIdentifier.initialize()
        val audioFeatures = createMockAudioFeatures()
        
        // Mock interpreter output for known speaker
        val mockOutput = Array(1) { FloatArray(3) { 0.0f } }
        mockOutput[0][0] = 0.9f // High confidence for speaker 0
        mockOutput[0][1] = 0.05f
        mockOutput[0][2] = 0.05f
        
        whenever(mockInterpreter.run(any(), any())).thenAnswer { invocation ->
            val output = invocation.getArgument<Array<FloatArray>>(1)
            output[0] = mockOutput[0]
        }
        
        // When
        val result = speakerIdentifier.identifySpeaker(audioFeatures)
        
        // Then
        assertNotNull("Should return identification result", result)
        assertEquals("Should identify speaker 0", "speaker_0", result?.speakerId)
        assertEquals("Should have high confidence", 0.9f, result?.confidence, 0.01f)
        assertTrue("Should be above threshold", result!!.confidence > 0.7f)
    }
    
    @Test
    fun testIdentifySpeaker_withUnknownSpeaker_shouldReturnUnknown() = runTest {
        // Given
        speakerIdentifier.initialize()
        val audioFeatures = createMockAudioFeatures()
        
        // Mock interpreter output for unknown speaker (low confidence for all)
        val mockOutput = Array(1) { FloatArray(3) { 0.33f } }
        
        whenever(mockInterpreter.run(any(), any())).thenAnswer { invocation ->
            val output = invocation.getArgument<Array<FloatArray>>(1)
            output[0] = mockOutput[0]
        }
        
        // When
        val result = speakerIdentifier.identifySpeaker(audioFeatures)
        
        // Then
        assertNotNull("Should return identification result", result)
        assertEquals("Should identify as unknown", "unknown", result?.speakerId)
        assertTrue("Should have low confidence", result!!.confidence < 0.7f)
    }
    
    @Test
    fun testIdentifySpeaker_withInvalidAudioFeatures_shouldReturnNull() = runTest {
        // Given
        speakerIdentifier.initialize()
        val invalidFeatures = FloatArray(0) // Empty features
        
        // When
        val result = speakerIdentifier.identifySpeaker(invalidFeatures)
        
        // Then
        assertNull("Should return null for invalid features", result)
    }
    
    @Test
    fun testIdentifySpeaker_withoutInitialization_shouldReturnNull() = runTest {
        // Given - not initialized
        val audioFeatures = createMockAudioFeatures()
        
        // When
        val result = speakerIdentifier.identifySpeaker(audioFeatures)
        
        // Then
        assertNull("Should return null without initialization", result)
    }
    
    @Test
    fun testEnrollSpeaker_shouldCreateNewSpeakerProfile() = runTest {
        // Given
        speakerIdentifier.initialize()
        val speakerId = "new_speaker"
        val audioSamples = listOf(
            createMockAudioFeatures(),
            createMockAudioFeatures(),
            createMockAudioFeatures()
        )
        
        // Mock successful enrollment
        whenever(mockInterpreter.run(any(), any())).thenAnswer { invocation ->
            val output = invocation.getArgument<Array<FloatArray>>(1)
            output[0] = FloatArray(128) { 0.5f } // Mock embedding
        }
        
        // When
        val result = speakerIdentifier.enrollSpeaker(speakerId, audioSamples)
        
        // Then
        assertTrue("Enrollment should succeed", result)
        verify(mockInterpreter, times(audioSamples.size)).run(any(), any())
    }
    
    @Test
    fun testEnrollSpeaker_withInsufficientSamples_shouldReturnFalse() = runTest {
        // Given
        speakerIdentifier.initialize()
        val speakerId = "new_speaker"
        val audioSamples = listOf(createMockAudioFeatures()) // Only 1 sample, need at least 3
        
        // When
        val result = speakerIdentifier.enrollSpeaker(speakerId, audioSamples)
        
        // Then
        assertFalse("Enrollment should fail with insufficient samples", result)
    }
    
    @Test
    fun testEnrollSpeaker_withExistingSpeaker_shouldUpdateProfile() = runTest {
        // Given
        speakerIdentifier.initialize()
        val speakerId = "existing_speaker"
        val audioSamples = listOf(
            createMockAudioFeatures(),
            createMockAudioFeatures(),
            createMockAudioFeatures()
        )
        
        // First enrollment
        whenever(mockInterpreter.run(any(), any())).thenAnswer { invocation ->
            val output = invocation.getArgument<Array<FloatArray>>(1)
            output[0] = FloatArray(128) { 0.5f }
        }
        speakerIdentifier.enrollSpeaker(speakerId, audioSamples)
        
        // When - second enrollment with same speaker ID
        val result = speakerIdentifier.enrollSpeaker(speakerId, audioSamples)
        
        // Then
        assertTrue("Re-enrollment should succeed", result)
    }
    
    @Test
    fun testGetEnrolledSpeakers_shouldReturnAllSpeakers() = runTest {
        // Given
        speakerIdentifier.initialize()
        val speakers = listOf("speaker1", "speaker2", "speaker3")
        val audioSamples = listOf(
            createMockAudioFeatures(),
            createMockAudioFeatures(),
            createMockAudioFeatures()
        )
        
        whenever(mockInterpreter.run(any(), any())).thenAnswer { invocation ->
            val output = invocation.getArgument<Array<FloatArray>>(1)
            output[0] = FloatArray(128) { 0.5f }
        }
        
        // Enroll speakers
        speakers.forEach { speakerId ->
            speakerIdentifier.enrollSpeaker(speakerId, audioSamples)
        }
        
        // When
        val enrolledSpeakers = speakerIdentifier.getEnrolledSpeakers()
        
        // Then
        assertEquals("Should return all enrolled speakers", 3, enrolledSpeakers.size)
        assertTrue("Should contain speaker1", enrolledSpeakers.contains("speaker1"))
        assertTrue("Should contain speaker2", enrolledSpeakers.contains("speaker2"))
        assertTrue("Should contain speaker3", enrolledSpeakers.contains("speaker3"))
    }
    
    @Test
    fun testRemoveSpeaker_shouldDeleteSpeakerProfile() = runTest {
        // Given
        speakerIdentifier.initialize()
        val speakerId = "speaker_to_remove"
        val audioSamples = listOf(
            createMockAudioFeatures(),
            createMockAudioFeatures(),
            createMockAudioFeatures()
        )
        
        whenever(mockInterpreter.run(any(), any())).thenAnswer { invocation ->
            val output = invocation.getArgument<Array<FloatArray>>(1)
            output[0] = FloatArray(128) { 0.5f }
        }
        
        // Enroll speaker first
        speakerIdentifier.enrollSpeaker(speakerId, audioSamples)
        assertTrue("Speaker should be enrolled", 
            speakerIdentifier.getEnrolledSpeakers().contains(speakerId))
        
        // When
        val result = speakerIdentifier.removeSpeaker(speakerId)
        
        // Then
        assertTrue("Removal should succeed", result)
        assertFalse("Speaker should no longer be enrolled", 
            speakerIdentifier.getEnrolledSpeakers().contains(speakerId))
    }
    
    @Test
    fun testRemoveSpeaker_withNonExistentSpeaker_shouldReturnFalse() = runTest {
        // Given
        speakerIdentifier.initialize()
        
        // When
        val result = speakerIdentifier.removeSpeaker("non_existent_speaker")
        
        // Then
        assertFalse("Removal should fail for non-existent speaker", result)
    }
    
    @Test
    fun testGetSpeakerEmbedding_shouldReturnEmbeddingVector() = runTest {
        // Given
        speakerIdentifier.initialize()
        val audioFeatures = createMockAudioFeatures()
        
        val mockEmbedding = FloatArray(128) { it * 0.01f }
        whenever(mockInterpreter.run(any(), any())).thenAnswer { invocation ->
            val output = invocation.getArgument<Array<FloatArray>>(1)
            output[0] = mockEmbedding
        }
        
        // When
        val embedding = speakerIdentifier.getSpeakerEmbedding(audioFeatures)
        
        // Then
        assertNotNull("Should return embedding", embedding)
        assertEquals("Should have correct size", 128, embedding?.size)
        assertArrayEquals("Should match mock embedding", mockEmbedding, embedding, 0.001f)
    }
    
    @Test
    fun testCalculateSimilarity_shouldReturnCorrectSimilarity() = runTest {
        // Given
        val embedding1 = FloatArray(128) { 1.0f }
        val embedding2 = FloatArray(128) { 1.0f }
        val embedding3 = FloatArray(128) { -1.0f }
        
        // When
        val similarity1 = speakerIdentifier.calculateSimilarity(embedding1, embedding2)
        val similarity2 = speakerIdentifier.calculateSimilarity(embedding1, embedding3)
        
        // Then
        assertEquals("Identical embeddings should have similarity 1.0", 1.0f, similarity1, 0.01f)
        assertEquals("Opposite embeddings should have similarity -1.0", -1.0f, similarity2, 0.01f)
    }
    
    @Test
    fun testUpdateConfidenceThreshold_shouldAffectIdentification() = runTest {
        // Given
        speakerIdentifier.initialize()
        val audioFeatures = createMockAudioFeatures()
        
        // Mock output with medium confidence
        val mockOutput = Array(1) { FloatArray(3) { 0.0f } }
        mockOutput[0][0] = 0.75f // Medium confidence
        mockOutput[0][1] = 0.125f
        mockOutput[0][2] = 0.125f
        
        whenever(mockInterpreter.run(any(), any())).thenAnswer { invocation ->
            val output = invocation.getArgument<Array<FloatArray>>(1)
            output[0] = mockOutput[0]
        }
        
        // When - with default threshold (0.7)
        val result1 = speakerIdentifier.identifySpeaker(audioFeatures)
        
        // Update threshold to higher value
        speakerIdentifier.updateConfidenceThreshold(0.8f)
        val result2 = speakerIdentifier.identifySpeaker(audioFeatures)
        
        // Then
        assertEquals("Should identify speaker with default threshold", 
            "speaker_0", result1?.speakerId)
        assertEquals("Should identify as unknown with higher threshold", 
            "unknown", result2?.speakerId)
    }
    
    @Test
    fun testCleanup_shouldReleaseResources() = runTest {
        // Given
        speakerIdentifier.initialize()
        
        // When
        speakerIdentifier.cleanup()
        
        // Then
        verify(mockInterpreter).close()
    }
    
    // Helper methods
    private fun createMockAudioFeatures(): FloatArray {
        return FloatArray(160) { it * 0.01f } // Mock MFCC features
    }
    
    // Mock interface for model loading (would be implemented in actual code)
    interface ModelLoader {
        fun loadModel(modelPath: String): Interpreter
    }
}