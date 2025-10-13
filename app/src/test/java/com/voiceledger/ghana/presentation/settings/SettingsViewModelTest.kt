package com.voiceledger.ghana.presentation.settings

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.voiceledger.ghana.data.local.entity.SpeakerProfile
import com.voiceledger.ghana.data.local.entity.Transaction
import com.voiceledger.ghana.domain.repository.SpeakerProfileRepository
import com.voiceledger.ghana.domain.repository.TransactionRepository
import com.voiceledger.ghana.service.VoiceAgentServiceManager
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var speakerProfileRepository: SpeakerProfileRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var voiceAgentServiceManager: VoiceAgentServiceManager
    private lateinit var viewModel: SettingsViewModel

    private val sampleSellerProfile = SpeakerProfile(
        id = "seller1",
        name = "Test Seller",
        voiceEmbedding = floatArrayOf(),
        isActive = true,
        visitCount = 0,
        totalSpent = 0.0,
        averageSpending = 0.0,
        lastVisit = System.currentTimeMillis(),
        customerType = "seller"
    )

    private val sampleTransactions = listOf(
        Transaction(
            id = "1",
            product = "Tilapia",
            amount = 25.0,
            timestamp = System.currentTimeMillis(),
            transcriptSnippet = "Two tilapia please",
            confidence = 0.9f,
            needsReview = false,
            quantity = 2.0,
            unit = "pieces",
            customerId = "customer1"
        ),
        Transaction(
            id = "2",
            product = "Mackerel",
            amount = 15.0,
            timestamp = System.currentTimeMillis() - 3600000,
            transcriptSnippet = "Give me mackerel",
            confidence = 0.85f,
            needsReview = true,
            quantity = 3.0,
            unit = "pieces",
            customerId = "customer2"
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        speakerProfileRepository = mockk()
        transactionRepository = mockk()
        voiceAgentServiceManager = mockk()

        // Mock repository calls
        every { transactionRepository.getAllTransactions() } returns flowOf(sampleTransactions)
        coEvery { speakerProfileRepository.getSellerProfile() } returns sampleSellerProfile
        coEvery { speakerProfileRepository.getCustomerCount() } returns 5
        coEvery { transactionRepository.deleteTransaction(any()) } just Runs
        coEvery { speakerProfileRepository.deleteProfile(any()) } just Runs
        every { speakerProfileRepository.getCustomerProfiles() } returns flowOf(emptyList())

        viewModel = SettingsViewModel(
            speakerProfileRepository,
            transactionRepository,
            voiceAgentServiceManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should load settings correctly`() = runTest {
        // Given - setup is done in @Before

        // When - viewModel is initialized

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertTrue(uiState.hasSellerProfile)
        assertEquals("Test Seller", uiState.sellerProfileName)
        assertEquals(2, uiState.totalTransactions)
        assertEquals(5, uiState.totalCustomers)
        assertEquals("English", uiState.settings.selectedLanguage.name)
    }

    @Test
    fun `update language should change selected language`() = runTest {
        // Given
        val twiLanguage = Language("tw", "Twi (Akan)")

        // When
        viewModel.updateLanguage(twiLanguage)

        // Then
        val uiState = viewModel.uiState.value
        assertEquals(twiLanguage, uiState.settings.selectedLanguage)
        assertEquals("Language updated to Twi (Akan)", uiState.message)
    }

    @Test
    fun `update market hours should validate and save hours`() = runTest {
        // Given
        val startHour = 7
        val endHour = 19

        // When
        viewModel.updateMarketHours(startHour, endHour)

        // Then
        val uiState = viewModel.uiState.value
        assertEquals(startHour, uiState.settings.marketStartHour)
        assertEquals(endHour, uiState.settings.marketEndHour)
        assertEquals("Market hours updated", uiState.message)
    }

    @Test
    fun `update market hours should reject invalid hours`() = runTest {
        // Given
        val startHour = 18
        val endHour = 6 // End before start

        // When
        viewModel.updateMarketHours(startHour, endHour)

        // Then
        val uiState = viewModel.uiState.value
        assertEquals("Start hour must be before end hour", uiState.error)
    }

    @Test
    fun `update notifications should toggle setting`() = runTest {
        // Given
        val enableNotifications = false

        // When
        viewModel.updateNotifications(enableNotifications)

        // Then
        val uiState = viewModel.uiState.value
        assertEquals(enableNotifications, uiState.settings.enableNotifications)
        assertEquals("Notifications disabled", uiState.message)
    }

    @Test
    fun `update voice confirmation should toggle setting`() = runTest {
        // Given
        val enableVoiceConfirmation = false

        // When
        viewModel.updateVoiceConfirmation(enableVoiceConfirmation)

        // Then
        val uiState = viewModel.uiState.value
        assertEquals(enableVoiceConfirmation, uiState.settings.enableVoiceConfirmation)
        assertEquals("Voice confirmation disabled", uiState.message)
    }

    @Test
    fun `update confidence threshold should validate and save threshold`() = runTest {
        // Given
        val threshold = 0.75f

        // When
        viewModel.updateConfidenceThreshold(threshold)

        // Then
        val uiState = viewModel.uiState.value
        assertEquals(threshold, uiState.settings.confidenceThreshold)
        assertEquals("Confidence threshold updated to 75%", uiState.message)
    }

    @Test
    fun `update confidence threshold should reject invalid values`() = runTest {
        // Given
        val invalidThreshold = 0.3f // Below minimum

        // When
        viewModel.updateConfidenceThreshold(invalidThreshold)

        // Then
        val uiState = viewModel.uiState.value
        assertEquals("Confidence threshold must be between 50% and 100%", uiState.error)
    }

    @Test
    fun `update auto backup should toggle setting`() = runTest {
        // Given
        val enableAutoBackup = true

        // When
        viewModel.updateAutoBackup(enableAutoBackup)

        // Then
        val uiState = viewModel.uiState.value
        assertEquals(enableAutoBackup, uiState.settings.autoBackup)
        assertEquals("Auto backup enabled", uiState.message)
    }

    @Test
    fun `update data retention should validate and save days`() = runTest {
        // Given
        val retentionDays = 180

        // When
        viewModel.updateDataRetention(retentionDays)

        // Then
        val uiState = viewModel.uiState.value
        assertEquals(retentionDays, uiState.settings.dataRetentionDays)
        assertEquals("Data retention updated to 180 days", uiState.message)
    }

    @Test
    fun `update data retention should reject invalid days`() = runTest {
        // Given
        val invalidDays = 20 // Below minimum

        // When
        viewModel.updateDataRetention(invalidDays)

        // Then
        val uiState = viewModel.uiState.value
        assertEquals("Data retention must be between 30 and 365 days", uiState.error)
    }

    @Test
    fun `start voice enrollment should update enrollment state`() = runTest {
        // When
        viewModel.startVoiceEnrollment()

        // Then - Initially should be enrolling
        assertTrue(viewModel.uiState.value.isEnrollingVoice)
        assertEquals(VoiceEnrollmentStep.RECORDING, viewModel.uiState.value.enrollmentStep)

        // After completion
        advanceUntilIdle()
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isEnrollingVoice)
        assertEquals(VoiceEnrollmentStep.COMPLETED, finalState.enrollmentStep)
        assertTrue(finalState.hasSellerProfile)
        assertEquals("Voice enrollment completed successfully", finalState.message)
    }

    @Test
    fun `cancel voice enrollment should reset enrollment state`() = runTest {
        // Given
        viewModel.startVoiceEnrollment()

        // When
        viewModel.cancelVoiceEnrollment()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isEnrollingVoice)
        assertEquals(VoiceEnrollmentStep.IDLE, uiState.enrollmentStep)
    }

    @Test
    fun `export data should simulate export process`() = runTest {
        // When
        viewModel.exportData()

        // Then - Initially should be exporting
        assertTrue(viewModel.uiState.value.isExportingData)

        // After completion
        advanceUntilIdle()
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isExportingData)
        assertEquals("Data exported successfully", finalState.message)
    }

    @Test
    fun `delete all data should remove transactions and customers`() = runTest {
        // When
        viewModel.deleteAllData()

        // Then - Initially should be deleting
        assertTrue(viewModel.uiState.value.isDeletingData)

        // After completion
        advanceUntilIdle()
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isDeletingData)
        assertEquals(0, finalState.totalTransactions)
        assertEquals(0, finalState.totalCustomers)
        assertEquals("All data deleted successfully", finalState.message)

        // Verify repository calls
        verify { transactionRepository.getAllTransactions() }
        coVerify { transactionRepository.deleteTransaction(any()) }
    }

    @Test
    fun `clear message should reset message state`() = runTest {
        // Given
        viewModel.updateLanguage(Language("tw", "Twi"))

        // When
        viewModel.clearMessage()

        // Then
        val uiState = viewModel.uiState.value
        assertEquals(null, uiState.message)
    }

    @Test
    fun `clear error should reset error state`() = runTest {
        // Given
        viewModel.updateConfidenceThreshold(0.3f) // Invalid threshold

        // When
        viewModel.clearError()

        // Then
        val uiState = viewModel.uiState.value
        assertEquals(null, uiState.error)
    }

    @Test
    fun `available languages should include Ghana languages`() {
        // When
        val languages = viewModel.availableLanguages

        // Then
        assertTrue(languages.any { it.code == "en" && it.name == "English" })
        assertTrue(languages.any { it.code == "tw" && it.name == "Twi (Akan)" })
        assertTrue(languages.any { it.code == "ga" && it.name == "Ga" })
        assertTrue(languages.any { it.code == "ee" && it.name == "Ewe" })
        assertTrue(languages.any { it.code == "dag" && it.name == "Dagbani" })
        assertTrue(languages.any { it.code == "ha" && it.name == "Hausa" })
    }

    @Test
    fun `settings should have sensible defaults`() = runTest {
        // When
        val settings = viewModel.uiState.value.settings

        // Then
        assertEquals("en", settings.selectedLanguage.code)
        assertEquals(6, settings.marketStartHour)
        assertEquals(18, settings.marketEndHour)
        assertTrue(settings.enableNotifications)
        assertTrue(settings.enableVoiceConfirmation)
        assertEquals(0.8f, settings.confidenceThreshold)
        assertFalse(settings.autoBackup)
        assertEquals(90, settings.dataRetentionDays)
    }

    @Test
    fun `error handling should update state correctly`() = runTest {
        // Given
        coEvery { speakerProfileRepository.getSellerProfile() } throws RuntimeException("Database error")

        // When
        val newViewModel = SettingsViewModel(
            speakerProfileRepository,
            transactionRepository,
            voiceAgentServiceManager
        )

        // Then
        val state = newViewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.error?.contains("Database error") == true)
    }
}