package com.voiceledger.ghana.ml.speaker

import android.content.Context
import com.voiceledger.ghana.domain.repository.SpeakerProfileRepository
import com.voiceledger.ghana.ml.audio.AudioUtils
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for speaker identification functionality
 * Tests enrollment, identification, and edge cases
 */
class SpeakerIdentificationTest {
    
    private lateinit var mockContext: Context
    private lateinit var mockSpeakerRepository: SpeakerProfileRepository
    private lateinit var mockAudioUtils: AudioUtils
    private lateinit var speakerIdentifier: MockSpeakerIdentifier
    
    @Before
    fun setup() {
        mockContext = mockk()
        mockSpeakerRepository = mockk()
        mockAudioUtils = mockk()
        
        speakerIdentifier = MockSpeakerIdentifier(mockContext, mockSpeakerRepository)
    }
    
    @After
    fun tearDown() {
        speakerIdentifier.cleanup()
        clearAllMocks()
    }
    
    @Test
    fun `initialize should succeed and update status`() = runTest {
        // When
        val result = speakerIdentifier.initialize()
        
        // Then
        assertTrue("Initialization should succeed", result.isSuccess)
        
        val status = speakerIdentifier.getStatus()
        status.collect { currentStatus ->
            assertTrue("Should be initialized", currentStatus.isInitialized)
            assertTrue("Model load time should be positive", currentStatus.modelLoadTime > 0)
        }
    }
    
    @Test
    fun `enrollSeller should fail when not initialized`() = runTest {
        // Given
        val audioSamples = listOf(
            ByteArray(1000) { 1 },
            ByteArray(1000) { 2 },
            ByteArray(1000) { 3 }
        )
        
        // When
        val result = speakerIdentifier.enrollSeller(audioSamples, "Test Seller")
        
        // Then
        assertFalse("Enrollment should fail when not initialized", result.success)
        assertEquals("Should have correct error message", 
            "Speaker identification not initialized", result.message)
    }
    
    @Test
    fun `enrollSeller should fail with insufficient samples`() = runTest {
        // Given
        speakerIdentifier.initialize()
        val audioSamples = listOf(
            ByteArray(1000) { 1 },
            ByteArray(1000) { 2 }
        ) // Only 2 samples, need at least 3
        
        // When
        val result = speakerIdentifier.enrollSeller(audioSamples, "Test Seller")
        
        // Then
        assertFalse("Enrollment should fail with insufficient samples", result.success)
        assertTrue("Error message should mention sample requirement", 
            result.message.contains("3 audio samples"))
        assertEquals("Should report correct samples processed", 2, result.samplesProcessed)
    }
    
    @Test
    fun `enrollSeller should succeed with sufficient samples`() = runTest {
        // Given
        speakerIdentifier.initialize()
        val audioSamples = listOf(
            ByteArray(1000) { 1 },
            ByteArray(1000) { 2 },
            ByteArray(1000) { 3 },
            ByteArray(1000) { 4 }
        )
        
        val mockProfile = mockk<com.voiceledger.ghana.data.local.entity.SpeakerProfile>()
        every { mockProfile.id } returns "seller_123"
        
        coEvery { mockSpeakerRepository.enrollSeller(any(), any()) } returns mockProfile
        
        // When
        val result = speakerIdentifier.enrollSeller(audioSamples, "Test Seller")
        
        // Then
        assertTrue("Enrollment should succeed", result.success)
        assertEquals("Should return seller ID", "seller_123", result.sellerId)
        assertTrue("Confidence should be reasonable", result.confidence >= 0.85f)
        assertEquals("Should process all samples", 4, result.samplesProcessed)
        
        coVerify { mockSpeakerRepository.enrollSeller(any(), "Test Seller") }
    }
    
    @Test
    fun `identifySpeaker should return unknown when not initialized`() = runTest {
        // Given
        val audioData = ByteArray(1000) { 1 }
        
        // When
        val result = speakerIdentifier.identifySpeaker(audioData)
        
        // Then
        assertEquals("Should return unknown speaker type", SpeakerType.UNKNOWN, result.speakerType)
        assertNull("Speaker ID should be null", result.speakerId)
        assertEquals("Confidence should be zero", 0f, result.confidence)
    }
    
    @Test
    fun `identifySpeaker should identify seller when profile exists`() = runTest {
        // Given
        speakerIdentifier.initialize()
        val audioData = ByteArray(1000) { 1 }
        
        val mockSellerProfile = mockk<com.voiceledger.ghana.data.local.entity.SpeakerProfile>()
        every { mockSellerProfile.id } returns "seller_123"
        
        coEvery { mockSpeakerRepository.getSellerProfile() } returns mockSellerProfile
        
        // When - Run multiple times to account for randomness in mock
        var sellerDetected = false
        repeat(10) {
            val result = speakerIdentifier.identifySpeaker(audioData)
            if (result.speakerType == SpeakerType.SELLER) {
                sellerDetected = true
                assertEquals("Should return seller ID", "seller_123", result.speakerId)
                assertTrue("Confidence should be high for seller", result.confidence >= 0.85f)
            }
        }
        
        // Then
        assertTrue("Should detect seller at least once in 10 attempts", sellerDetected)
    }
    
    @Test
    fun `identifySpeaker should handle new customers`() = runTest {
        // Given
        speakerIdentifier.initialize()
        val audioData = ByteArray(1000) { 1 }
        
        coEvery { mockSpeakerRepository.getSellerProfile() } returns null
        
        // When - Run multiple times to get different results
        var newCustomerDetected = false
        repeat(20) {
            val result = speakerIdentifier.identifySpeaker(audioData)
            if (result.speakerType == SpeakerType.NEW_CUSTOMER) {
                newCustomerDetected = true
                assertNotNull("New customer should have ID", result.speakerId)
                assertTrue("Should be marked as new customer", result.isNewCustomer)
                assertEquals("Visit count should be 1", 1, result.customerVisitCount)
                assertTrue("Confidence should be reasonable", result.confidence >= 0.6f)
            }
        }
        
        // Then
        assertTrue("Should detect new customer at least once", newCustomerDetected)
    }
    
    @Test
    fun `addCustomerProfile should succeed`() = runTest {
        // Given
        val embedding = FloatArray(128) { it.toFloat() }
        val customerId = "customer_123"
        
        coEvery { mockSpeakerRepository.addCustomerProfile(embedding, customerId) } returns mockk()
        coEvery { mockSpeakerRepository.getCustomerCount() } returns 5
        
        // When
        val result = speakerIdentifier.addCustomerProfile(embedding, customerId)
        
        // Then
        assertTrue("Should succeed", result.isSuccess)
        coVerify { mockSpeakerRepository.addCustomerProfile(embedding, customerId) }
    }
    
    @Test
    fun `updateSellerProfile should fail when not initialized`() = runTest {
        // Given
        val audioData = ByteArray(1000) { 1 }
        
        // When
        val result = speakerIdentifier.updateSellerProfile(audioData)
        
        // Then
        assertTrue("Should fail when not initialized", result.isFailure)
        assertEquals("Should have correct error message", 
            "Not initialized", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `updateSellerProfile should fail when no seller profile exists`() = runTest {
        // Given
        speakerIdentifier.initialize()
        val audioData = ByteArray(1000) { 1 }
        
        coEvery { mockSpeakerRepository.getSellerProfile() } returns null
        
        // When
        val result = speakerIdentifier.updateSellerProfile(audioData)
        
        // Then
        assertTrue("Should fail when no seller profile", result.isFailure)
        assertEquals("Should have correct error message", 
            "No seller profile found", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `updateSellerProfile should succeed when seller profile exists`() = runTest {
        // Given
        speakerIdentifier.initialize()
        val audioData = ByteArray(1000) { 1 }
        
        val mockSellerProfile = mockk<com.voiceledger.ghana.data.local.entity.SpeakerProfile>()
        every { mockSellerProfile.id } returns "seller_123"
        
        coEvery { mockSpeakerRepository.getSellerProfile() } returns mockSellerProfile
        coEvery { mockSpeakerRepository.updateVoiceEmbedding(any(), any()) } just Runs
        
        // When
        val result = speakerIdentifier.updateSellerProfile(audioData)
        
        // Then
        assertTrue("Should succeed", result.isSuccess)
        coVerify { mockSpeakerRepository.updateVoiceEmbedding("seller_123", any()) }
    }
    
    @Test
    fun `cleanup should reset initialization status`() = runTest {
        // Given
        speakerIdentifier.initialize()
        
        // When
        speakerIdentifier.cleanup()
        
        // Then
        val status = speakerIdentifier.getStatus()
        status.collect { currentStatus ->
            assertFalse("Should not be initialized after cleanup", currentStatus.isInitialized)
        }
    }
    
    @Test
    fun `status should update correctly during operations`() = runTest {
        // Given
        speakerIdentifier.initialize()
        
        // When - Perform various operations
        val audioSamples = listOf(
            ByteArray(1000) { 1 },
            ByteArray(1000) { 2 },
            ByteArray(1000) { 3 }
        )
        
        val mockProfile = mockk<com.voiceledger.ghana.data.local.entity.SpeakerProfile>()
        every { mockProfile.id } returns "seller_123"
        coEvery { mockSpeakerRepository.enrollSeller(any(), any()) } returns mockProfile
        
        speakerIdentifier.enrollSeller(audioSamples, "Test Seller")
        
        // Then
        val status = speakerIdentifier.getStatus()
        status.collect { currentStatus ->
            assertTrue("Should be initialized", currentStatus.isInitialized)
            assertTrue("Should have seller profile", currentStatus.hasSellerProfile)
            assertTrue("Model load time should be positive", currentStatus.modelLoadTime > 0)
        }
    }
}