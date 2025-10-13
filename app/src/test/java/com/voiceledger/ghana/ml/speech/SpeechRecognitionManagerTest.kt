package com.voiceledger.ghana.ml.speech

import android.content.Context
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

/**
 * Unit tests for SpeechRecognitionManager
 * Tests automatic online/offline switching and Ghana language support
 */
@ExperimentalCoroutinesApi
class SpeechRecognitionManagerTest {
    
    @Mock
    private lateinit var context: Context
    
    @Mock
    private lateinit var googleSpeechRecognizer: GoogleCloudSpeechRecognizer
    
    @Mock
    private lateinit var offlineSpeechRecognizer: OfflineSpeechRecognizer
    
    private lateinit var speechRecognitionManager: SpeechRecognitionManager
    
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        
        // Setup default mock behaviors
        `when`(googleSpeechRecognizer.getSupportedLanguages()).thenReturn(
            listOf("en-GH", "tw", "ga", "en-US", "en-GB")
        )
        `when`(offlineSpeechRecognizer.getSupportedLanguages()).thenReturn(
            listOf("en", "en-GH")
        )
        `when`(offlineSpeechRecognizer.isOfflineCapable()).thenReturn(true)
        
        speechRecognitionManager = SpeechRecognitionManager(
            context = context,
            googleSpeechRecognizer = googleSpeechRecognizer,
            offlineSpeechRecognizer = offlineSpeechRecognizer
        )
    }
    
    @Test
    fun `transcribe should use online recognizer when network available`() = runTest {
        // Given
        val audioData = ByteArray(1000)
        val expectedResult = TranscriptionResult(
            transcript = "how much is this fish",
            confidence = 0.9f,
            detectedLanguage = "en-GH",
            processingTimeMs = 500
        )
        
        `when`(googleSpeechRecognizer.transcribe(any(), any())).thenReturn(expectedResult)
        
        // When
        val result = speechRecognitionManager.transcribe(audioData)
        
        // Then
        assertEquals(expectedResult, result)
        verify(googleSpeechRecognizer).transcribe(eq(audioData), any())
        verify(offlineSpeechRecognizer, never()).transcribe(any(), any())
    }
    
    @Test
    fun `transcribe should use offline recognizer when network unavailable`() = runTest {
        // Given
        val audioData = ByteArray(1000)
        val expectedResult = TranscriptionResult(
            transcript = "how much",
            confidence = 0.7f,
            detectedLanguage = "en",
            processingTimeMs = 800
        )
        
        speechRecognitionManager.setPreferOnlineRecognition(false)
        `when`(offlineSpeechRecognizer.transcribe(any(), any())).thenReturn(expectedResult)
        
        // When
        val result = speechRecognitionManager.transcribe(audioData)
        
        // Then
        assertEquals(expectedResult, result)
        verify(offlineSpeechRecognizer).transcribe(eq(audioData), any())
        verify(googleSpeechRecognizer, never()).transcribe(any(), any())
    }
    
    @Test
    fun `setLanguagePreferences should update both recognizers`() {
        // Given
        val languages = listOf("en-GH", "tw", "ga")
        
        // When
        speechRecognitionManager.setLanguagePreferences(languages)
        
        // Then
        verify(googleSpeechRecognizer).setLanguageModel(languages)
        verify(offlineSpeechRecognizer).setLanguageModel(languages)
    }
    
    @Test
    fun `setCodeSwitchingEnabled should update both recognizers`() {
        // Given
        val enabled = true
        
        // When
        speechRecognitionManager.setCodeSwitchingEnabled(enabled)
        
        // Then
        verify(googleSpeechRecognizer).setCodeSwitchingEnabled(enabled)
        verify(offlineSpeechRecognizer).setCodeSwitchingEnabled(enabled)
    }
    
    @Test
    fun `getSupportedLanguages should combine languages from both recognizers`() {
        // When
        val supportedLanguages = speechRecognitionManager.getSupportedLanguages()
        
        // Then
        assertTrue("Should include Ghana English", supportedLanguages.contains("en-GH"))
        assertTrue("Should include Twi", supportedLanguages.contains("tw"))
        assertTrue("Should include Ga", supportedLanguages.contains("ga"))
        assertTrue("Should include English", supportedLanguages.contains("en-US"))
    }
    
    @Test
    fun `isLanguageSupported should check both recognizers`() {
        // Given
        `when`(googleSpeechRecognizer.isLanguageSupported("en-GH")).thenReturn(true)
        `when`(offlineSpeechRecognizer.isLanguageSupported("en-GH")).thenReturn(true)
        `when`(googleSpeechRecognizer.isLanguageSupported("fr")).thenReturn(false)
        `when`(offlineSpeechRecognizer.isLanguageSupported("fr")).thenReturn(false)
        
        // When & Then
        assertTrue("Should support Ghana English", speechRecognitionManager.isLanguageSupported("en-GH"))
        assertFalse("Should not support French", speechRecognitionManager.isLanguageSupported("fr"))
    }
    
    @Test
    fun `getRecognitionCapabilities should return correct capabilities`() {
        // When
        val capabilities = speechRecognitionManager.getRecognitionCapabilities()
        
        // Then
        assertTrue("Should support offline", capabilities.offlineAvailable)
        assertTrue("Should support streaming", capabilities.streamingSupported)
        assertTrue("Should support code-switching", capabilities.codeSwitchingSupported)
        assertFalse("Should not have network by default", capabilities.onlineAvailable)
    }
    
    @Test
    fun `getGhanaLanguageSuggestions should detect Twi patterns`() {
        // Given
        val twiTranscript = "sɛn na ɛyɛ this fish"
        
        // When
        val suggestions = speechRecognitionManager.getGhanaLanguageSuggestions(twiTranscript)
        
        // Then
        assertTrue("Should suggest Twi", suggestions.contains("tw"))
    }
    
    @Test
    fun `getGhanaLanguageSuggestions should detect Ga patterns`() {
        // Given
        val gaTranscript = "bawo naa this fish"
        
        // When
        val suggestions = speechRecognitionManager.getGhanaLanguageSuggestions(gaTranscript)
        
        // Then
        assertTrue("Should suggest Ga", suggestions.contains("ga"))
    }
    
    @Test
    fun `getGhanaLanguageSuggestions should detect Ghana English patterns`() {
        // Given
        val ghanaEnglishTranscript = "how much reduce small"
        
        // When
        val suggestions = speechRecognitionManager.getGhanaLanguageSuggestions(ghanaEnglishTranscript)
        
        // Then
        assertTrue("Should suggest Ghana English", suggestions.contains("en-GH"))
    }
    
    @Test
    fun `getGhanaLanguageSuggestions should default to Ghana English`() {
        // Given
        val neutralTranscript = "hello world"
        
        // When
        val suggestions = speechRecognitionManager.getGhanaLanguageSuggestions(neutralTranscript)
        
        // Then
        assertEquals("Should default to Ghana English", listOf("en-GH"), suggestions)
    }
    
    @Test
    fun `optimizeForMarketEnvironment should set Ghana languages`() {
        // When
        speechRecognitionManager.optimizeForMarketEnvironment()
        
        // Then
        verify(googleSpeechRecognizer).setLanguageModel(listOf("en-GH", "tw", "ga"))
        verify(offlineSpeechRecognizer).setLanguageModel(listOf("en-GH", "tw", "ga"))
        verify(googleSpeechRecognizer).setCodeSwitchingEnabled(true)
        verify(offlineSpeechRecognizer).setCodeSwitchingEnabled(true)
    }
    
    @Test
    fun `transcribe should handle recognition errors gracefully`() = runTest {
        // Given
        val audioData = ByteArray(1000)
        `when`(googleSpeechRecognizer.transcribe(any(), any())).thenThrow(RuntimeException("Network error"))
        
        // When
        val result = speechRecognitionManager.transcribe(audioData)
        
        // Then
        assertFalse("Should not be successful", result.isSuccess)
        assertNotNull("Should have error message", result.error)
        assertTrue("Should contain error info", result.error!!.contains("Network error"))
    }
    
    @Test
    fun `startStreamingRecognition should use online recognizer by default`() = runTest {
        // Given
        val mockSession = mock(StreamingSession::class.java)
        `when`(googleSpeechRecognizer.startStreaming(any(), any())).thenReturn(mockSession)
        
        // When
        val session = speechRecognitionManager.startStreamingRecognition { }
        
        // Then
        assertEquals(mockSession, session)
        verify(googleSpeechRecognizer).startStreaming(any(), any())
        verify(offlineSpeechRecognizer, never()).startStreaming(any(), any())
    }
    
    @Test
    fun `startStreamingRecognition should use offline recognizer when preferred`() = runTest {
        // Given
        val mockSession = mock(StreamingSession::class.java)
        speechRecognitionManager.setPreferOnlineRecognition(false)
        `when`(offlineSpeechRecognizer.startStreaming(any(), any())).thenReturn(mockSession)
        
        // When
        val session = speechRecognitionManager.startStreamingRecognition { }
        
        // Then
        assertEquals(mockSession, session)
        verify(offlineSpeechRecognizer).startStreaming(any(), any())
        verify(googleSpeechRecognizer, never()).startStreaming(any(), any())
    }
    
    @Test
    fun `cleanup should cleanup both recognizers`() {
        // When
        speechRecognitionManager.cleanup()
        
        // Then
        verify(googleSpeechRecognizer).cleanup()
        verify(offlineSpeechRecognizer).cleanup()
    }
    
    // Helper method to create any() matcher for ByteArray
    private fun any(): ByteArray = org.mockito.kotlin.any()
}