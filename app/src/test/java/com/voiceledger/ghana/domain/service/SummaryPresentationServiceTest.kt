package com.voiceledger.ghana.domain.service

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.voiceledger.ghana.data.local.entity.DailySummary
import com.voiceledger.ghana.domain.repository.DailySummaryRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SummaryPresentationServiceTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var context: Context
    private lateinit var dailySummaryRepository: DailySummaryRepository
    private lateinit var summaryPresentationService: SummaryPresentationService

    private val sampleSummary = DailySummary(
        date = "2024-01-15",
        totalSales = 250.0,
        transactionCount = 12,
        uniqueCustomers = 8,
        topProduct = "Tilapia",
        topProductSales = 120.0,
        peakHour = "14",
        peakHourSales = 80.0,
        averageTransactionValue = 20.83,
        repeatCustomers = 3,
        newCustomers = 2,
        totalQuantitySold = 25.0,
        mostProfitableHour = "14",
        leastActiveHour = "16",
        confidenceScore = 0.87f,
        reviewedTransactions = 10,
        comparisonWithYesterday = ComparisonData(
            salesChange = 15.5,
            transactionCountChange = 20.0,
            averageValueChange = 2.5
        ),
        comparisonWithLastWeek = ComparisonData(
            salesChange = -5.2,
            transactionCountChange = 8.3,
            averageValueChange = -1.2
        ),
        productBreakdown = mapOf(
            "Tilapia" to ProductSummary(
                totalSales = 120.0,
                transactionCount = 6,
                totalQuantity = 12.0,
                averagePrice = 10.0,
                peakHour = "14"
            ),
            "Mackerel" to ProductSummary(
                totalSales = 80.0,
                transactionCount = 4,
                totalQuantity = 8.0,
                averagePrice = 10.0,
                peakHour = "15"
            )
        ),
        hourlyBreakdown = mapOf(
            "14" to HourlySummary(
                totalSales = 80.0,
                transactionCount = 4,
                uniqueCustomers = 3,
                topProduct = "Tilapia"
            )
        ),
        customerInsights = mapOf(
            "customer1" to CustomerInsight(
                totalSpent = 50.0,
                transactionCount = 3,
                favoriteProduct = "Tilapia",
                averageTransactionValue = 16.67,
                preferredTime = "14"
            )
        ),
        recommendations = listOf(
            "Your peak sales hour is 14:00. Consider stocking more inventory during this time.",
            "Focus on customer retention strategies to increase repeat business."
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        context = mockk(relaxed = true)
        dailySummaryRepository = mockk()
        
        // Mock TextToSpeech initialization
        mockkConstructor(TextToSpeech::class)
        every { anyConstructed<TextToSpeech>().setLanguage(any()) } returns TextToSpeech.SUCCESS
        every { anyConstructed<TextToSpeech>().speak(any(), any(), any(), any()) } returns TextToSpeech.SUCCESS
        every { anyConstructed<TextToSpeech>().stop() } returns TextToSpeech.SUCCESS
        every { anyConstructed<TextToSpeech>().shutdown() } just Runs
        every { anyConstructed<TextToSpeech>().setSpeechRate(any()) } returns TextToSpeech.SUCCESS
        every { anyConstructed<TextToSpeech>().setPitch(any()) } returns TextToSpeech.SUCCESS
        every { anyConstructed<TextToSpeech>().setOnUtteranceProgressListener(any()) } just Runs

        summaryPresentationService = SummaryPresentationService(context, dailySummaryRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `presentDailySummary should generate English presentation correctly`() = runTest {
        // When
        val result = summaryPresentationService.presentDailySummary(
            summary = sampleSummary,
            includeVoice = false,
            language = "en"
        )

        // Then
        assertTrue(result is PresentationResult.Success)
        val presentation = (result as PresentationResult.Success).presentation
        
        assertEquals("en", presentation.language)
        assertTrue(presentation.title.contains("Daily Sales Summary"))
        assertTrue(presentation.sections.isNotEmpty())
        assertTrue(presentation.voiceScript.isNotEmpty())
        assertTrue(presentation.duration > 0)
        
        // Check that all section types are present
        val sectionTypes = presentation.sections.map { it.type }.toSet()
        assertTrue(sectionTypes.contains(SectionType.HEADER))
        assertTrue(sectionTypes.contains(SectionType.METRICS))
        assertTrue(sectionTypes.contains(SectionType.PRODUCTS))
        assertTrue(sectionTypes.contains(SectionType.CUSTOMERS))
        assertTrue(sectionTypes.contains(SectionType.TIME))
        assertTrue(sectionTypes.contains(SectionType.COMPARISON))
        assertTrue(sectionTypes.contains(SectionType.RECOMMENDATIONS))
    }

    @Test
    fun `presentDailySummary should generate Twi presentation correctly`() = runTest {
        // When
        val result = summaryPresentationService.presentDailySummary(
            summary = sampleSummary,
            includeVoice = false,
            language = "tw"
        )

        // Then
        assertTrue(result is PresentationResult.Success)
        val presentation = (result as PresentationResult.Success).presentation
        
        assertEquals("tw", presentation.language)
        assertTrue(presentation.title.contains("Nnɛ Adwuma Akontaabu"))
        assertTrue(presentation.voiceScript.contains("cedis"))
    }

    @Test
    fun `presentDailySummary should generate Ga presentation correctly`() = runTest {
        // When
        val result = summaryPresentationService.presentDailySummary(
            summary = sampleSummary,
            includeVoice = false,
            language = "ga"
        )

        // Then
        assertTrue(result is PresentationResult.Success)
        val presentation = (result as PresentationResult.Success).presentation
        
        assertEquals("ga", presentation.language)
        assertTrue(presentation.title.contains("Ŋmɛ Adwuma Akɔŋ"))
    }

    @Test
    fun `generateQuickSummary should create concise summary`() = runTest {
        // When
        val quickSummary = summaryPresentationService.generateQuickSummary(sampleSummary)

        // Then
        assertTrue(quickSummary.contains("250"))
        assertTrue(quickSummary.contains("12"))
        assertTrue(quickSummary.contains("8"))
        assertTrue(quickSummary.contains("cedis"))
    }

    @Test
    fun `generateQuickSummary should handle zero sales`() = runTest {
        // Given
        val emptySummary = sampleSummary.copy(
            totalSales = 0.0,
            transactionCount = 0,
            uniqueCustomers = 0
        )

        // When
        val quickSummary = summaryPresentationService.generateQuickSummary(emptySummary)

        // Then
        assertEquals("No sales recorded today.", quickSummary)
    }

    @Test
    fun `voice script should include key metrics`() = runTest {
        // When
        val result = summaryPresentationService.presentDailySummary(
            summary = sampleSummary,
            includeVoice = false,
            language = "en"
        )

        // Then
        assertTrue(result is PresentationResult.Success)
        val presentation = (result as PresentationResult.Success).presentation
        val voiceScript = presentation.voiceScript
        
        assertTrue(voiceScript.contains("250"))
        assertTrue(voiceScript.contains("12 transactions"))
        assertTrue(voiceScript.contains("8 customers"))
        assertTrue(voiceScript.contains("Tilapia"))
        assertTrue(voiceScript.contains("14 o'clock"))
        assertTrue(voiceScript.contains("repeat customers"))
    }

    @Test
    fun `voice script should include comparison data`() = runTest {
        // When
        val result = summaryPresentationService.presentDailySummary(
            summary = sampleSummary,
            includeVoice = false,
            language = "en"
        )

        // Then
        assertTrue(result is PresentationResult.Success)
        val presentation = (result as PresentationResult.Success).presentation
        val voiceScript = presentation.voiceScript
        
        assertTrue(voiceScript.contains("up 16 percent") || voiceScript.contains("up 15"))
        assertTrue(voiceScript.contains("Great job"))
    }

    @Test
    fun `voice script should include recommendations`() = runTest {
        // When
        val result = summaryPresentationService.presentDailySummary(
            summary = sampleSummary,
            includeVoice = false,
            language = "en"
        )

        // Then
        assertTrue(result is PresentationResult.Success)
        val presentation = (result as PresentationResult.Success).presentation
        val voiceScript = presentation.voiceScript
        
        assertTrue(voiceScript.contains("Here's a tip:"))
        assertTrue(voiceScript.contains("peak sales hour"))
    }

    @Test
    fun `presentation should handle empty summary gracefully`() = runTest {
        // Given
        val emptySummary = sampleSummary.copy(
            totalSales = 0.0,
            transactionCount = 0,
            uniqueCustomers = 0,
            topProduct = null,
            peakHour = null,
            recommendations = emptyList()
        )

        // When
        val result = summaryPresentationService.presentDailySummary(
            summary = emptySummary,
            includeVoice = false,
            language = "en"
        )

        // Then
        assertTrue(result is PresentationResult.Success)
        val presentation = (result as PresentationResult.Success).presentation
        
        assertTrue(presentation.sections.isNotEmpty())
        assertTrue(presentation.voiceScript.contains("No sales were recorded"))
    }

    @Test
    fun `setSpeechRate should update TTS settings`() {
        // When
        summaryPresentationService.setSpeechRate(1.5f)

        // Then
        verify { anyConstructed<TextToSpeech>().setSpeechRate(1.5f) }
    }

    @Test
    fun `setSpeechPitch should update TTS settings`() {
        // When
        summaryPresentationService.setSpeechPitch(0.8f)

        // Then
        verify { anyConstructed<TextToSpeech>().setPitch(0.8f) }
    }

    @Test
    fun `stopSpeaking should stop TTS`() {
        // When
        summaryPresentationService.stopSpeaking()

        // Then
        verify { anyConstructed<TextToSpeech>().stop() }
    }

    @Test
    fun `cleanup should shutdown TTS`() {
        // When
        summaryPresentationService.cleanup()

        // Then
        verify { anyConstructed<TextToSpeech>().stop() }
        verify { anyConstructed<TextToSpeech>().shutdown() }
    }

    @Test
    fun `presentation should format currency correctly`() = runTest {
        // When
        val result = summaryPresentationService.presentDailySummary(
            summary = sampleSummary,
            includeVoice = false,
            language = "en"
        )

        // Then
        assertTrue(result is PresentationResult.Success)
        val presentation = (result as PresentationResult.Success).presentation
        
        val metricsSection = presentation.sections.find { it.type == SectionType.METRICS }
        assertNotNull(metricsSection)
        assertTrue(metricsSection.content.contains("GH₵") || metricsSection.content.contains("250"))
    }

    @Test
    fun `presentation should include product breakdown`() = runTest {
        // When
        val result = summaryPresentationService.presentDailySummary(
            summary = sampleSummary,
            includeVoice = false,
            language = "en"
        )

        // Then
        assertTrue(result is PresentationResult.Success)
        val presentation = (result as PresentationResult.Success).presentation
        
        val productSection = presentation.sections.find { it.type == SectionType.PRODUCTS }
        assertNotNull(productSection)
        assertTrue(productSection.content.contains("Tilapia"))
        assertTrue(productSection.content.contains("Mackerel"))
    }

    @Test
    fun `presentation should include time analysis`() = runTest {
        // When
        val result = summaryPresentationService.presentDailySummary(
            summary = sampleSummary,
            includeVoice = false,
            language = "en"
        )

        // Then
        assertTrue(result is PresentationResult.Success)
        val presentation = (result as PresentationResult.Success).presentation
        
        val timeSection = presentation.sections.find { it.type == SectionType.TIME }
        assertNotNull(timeSection)
        assertTrue(timeSection.content.contains("14:00"))
    }

    @Test
    fun `voice script should handle large amounts correctly`() = runTest {
        // Given
        val largeSummary = sampleSummary.copy(totalSales = 15000.0)

        // When
        val result = summaryPresentationService.presentDailySummary(
            summary = largeSummary,
            includeVoice = false,
            language = "en"
        )

        // Then
        assertTrue(result is PresentationResult.Success)
        val presentation = (result as PresentationResult.Success).presentation
        assertTrue(presentation.voiceScript.contains("15 thousand cedis"))
    }

    @Test
    fun `duration estimation should be reasonable`() = runTest {
        // When
        val result = summaryPresentationService.presentDailySummary(
            summary = sampleSummary,
            includeVoice = false,
            language = "en"
        )

        // Then
        assertTrue(result is PresentationResult.Success)
        val presentation = (result as PresentationResult.Success).presentation
        assertTrue(presentation.duration > 0)
        assertTrue(presentation.duration < 300) // Should be less than 5 minutes
    }
}empt
yList()
        )

        // When
        val result = summaryPresentationService.presentDailySummary(
            summary = emptySummary,
            includeVoice = false,
            language = "en"
        )

        // Then
        assertTrue(result is PresentationResult.Success)
        val presentation = (result as PresentationResult.Success).presentation
        
        // Should still have basic sections
        assertTrue(presentation.sections.isNotEmpty())
        val headerSection = presentation.sections.find { it.type == SectionType.HEADER }
        assertNotNull(headerSection)
    }

    @Test
    fun `presentation should format currency correctly`() = runTest {
        // When
        val result = summaryPresentationService.presentDailySummary(
            summary = sampleSummary,
            includeVoice = false,
            language = "en"
        )

        // Then
        assertTrue(result is PresentationResult.Success)
        val presentation = (result as PresentationResult.Success).presentation
        
        val metricsSection = presentation.sections.find { it.type == SectionType.METRICS }
        assertNotNull(metricsSection)
        
        // Should contain formatted currency
        assertTrue(metricsSection.content.contains("GH₵") || metricsSection.content.contains("250"))
    }

    @Test
    fun `presentation should include product breakdown`() = runTest {
        // When
        val result = summaryPresentationService.presentDailySummary(
            summary = sampleSummary,
            includeVoice = false,
            language = "en"
        )

        // Then
        assertTrue(result is PresentationResult.Success)
        val presentation = (result as PresentationResult.Success).presentation
        
        val productSection = presentation.sections.find { it.type == SectionType.PRODUCTS }
        assertNotNull(productSection)
        assertTrue(productSection.content.contains("Tilapia"))
        assertTrue(productSection.content.contains("Mackerel"))
    }

    @Test
    fun `presentation should include customer insights`() = runTest {
        // When
        val result = summaryPresentationService.presentDailySummary(
            summary = sampleSummary,
            includeVoice = false,
            language = "en"
        )

        // Then
        assertTrue(result is PresentationResult.Success)
        val presentation = (result as PresentationResult.Success).presentation
        
        val customerSection = presentation.sections.find { it.type == SectionType.CUSTOMERS }
        assertNotNull(customerSection)
        assertTrue(customerSection.content.contains("8"))
        assertTrue(customerSection.content.contains("3"))
        assertTrue(customerSection.content.contains("2"))
    }

    @Test
    fun `presentation should include time analysis`() = runTest {
        // When
        val result = summaryPresentationService.presentDailySummary(
            summary = sampleSummary,
            includeVoice = false,
            language = "en"
        )

        // Then
        assertTrue(result is PresentationResult.Success)
        val presentation = (result as PresentationResult.Success).presentation
        
        val timeSection = presentation.sections.find { it.type == SectionType.TIME }
        assertNotNull(timeSection)
        assertTrue(timeSection.content.contains("14:00"))
    }

    @Test
    fun `presentation should include recommendations`() = runTest {
        // When
        val result = summaryPresentationService.presentDailySummary(
            summary = sampleSummary,
            includeVoice = false,
            language = "en"
        )

        // Then
        assertTrue(result is PresentationResult.Success)
        val presentation = (result as PresentationResult.Success).presentation
        
        val recommendationsSection = presentation.sections.find { it.type == SectionType.RECOMMENDATIONS }
        assertNotNull(recommendationsSection)
        assertTrue(recommendationsSection.content.contains("peak sales hour"))
        assertTrue(recommendationsSection.content.contains("customer retention"))
    }

    @Test
    fun `speech controls should work correctly`() = runTest {
        // Test speech rate
        summaryPresentationService.setSpeechRate(1.5f)
        verify { anyConstructed<TextToSpeech>().setSpeechRate(1.5f) }

        // Test speech pitch
        summaryPresentationService.setSpeechPitch(0.8f)
        verify { anyConstructed<TextToSpeech>().setPitch(0.8f) }

        // Test stop speaking
        summaryPresentationService.stopSpeaking()
        verify { anyConstructed<TextToSpeech>().stop() }
    }

    @Test
    fun `speech rate should be clamped to valid range`() = runTest {
        // Test upper bound
        summaryPresentationService.setSpeechRate(3.0f)
        verify { anyConstructed<TextToSpeech>().setSpeechRate(2.0f) }

        // Test lower bound
        summaryPresentationService.setSpeechRate(0.1f)
        verify { anyConstructed<TextToSpeech>().setSpeechRate(0.5f) }
    }

    @Test
    fun `speech pitch should be clamped to valid range`() = runTest {
        // Test upper bound
        summaryPresentationService.setSpeechPitch(3.0f)
        verify { anyConstructed<TextToSpeech>().setPitch(2.0f) }

        // Test lower bound
        summaryPresentationService.setSpeechPitch(0.1f)
        verify { anyConstructed<TextToSpeech>().setPitch(0.5f) }
    }

    @Test
    fun `cleanup should properly shutdown TTS`() = runTest {
        // When
        summaryPresentationService.cleanup()

        // Then
        verify { anyConstructed<TextToSpeech>().stop() }
        verify { anyConstructed<TextToSpeech>().shutdown() }
    }

    @Test
    fun `duration estimation should be reasonable`() = runTest {
        // When
        val result = summaryPresentationService.presentDailySummary(
            summary = sampleSummary,
            includeVoice = false,
            language = "en"
        )

        // Then
        assertTrue(result is PresentationResult.Success)
        val presentation = (result as PresentationResult.Success).presentation
        
        // Duration should be reasonable (not 0, not too long)
        assertTrue(presentation.duration > 0)
        assertTrue(presentation.duration < 300) // Less than 5 minutes
    }

    @Test
    fun `voice script should handle large amounts correctly`() = runTest {
        // Given
        val largeSummary = sampleSummary.copy(
            totalSales = 15000.0,
            transactionCount = 150
        )

        // When
        val result = summaryPresentationService.presentDailySummary(
            summary = largeSummary,
            includeVoice = false,
            language = "en"
        )

        // Then
        assertTrue(result is PresentationResult.Success)
        val presentation = (result as PresentationResult.Success).presentation
        val voiceScript = presentation.voiceScript
        
        assertTrue(voiceScript.contains("15 thousand cedis") || voiceScript.contains("15000"))
    }

    @Test
    fun `error handling should work for TTS failures`() = runTest {
        // Given - Mock TTS to fail
        every { anyConstructed<TextToSpeech>().speak(any(), any(), any(), any()) } returns TextToSpeech.ERROR

        // When
        val result = summaryPresentationService.presentDailySummary(
            summary = sampleSummary,
            includeVoice = true,
            language = "en"
        )

        // Then - Should still succeed with presentation, just voice might fail
        assertTrue(result is PresentationResult.Success)
    }
}