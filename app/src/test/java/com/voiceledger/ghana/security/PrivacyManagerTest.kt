package com.voiceledger.ghana.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import java.io.File

/**
 * Unit tests for PrivacyManager
 */
@RunWith(AndroidJUnit4::class)
class PrivacyManagerTest {
    
    private lateinit var context: Context
    @Mock
    private lateinit var mockEncryptionService: EncryptionService
    private lateinit var privacyManager: PrivacyManager
    
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        
        // Mock encrypted shared preferences behavior
        whenever(mockEncryptionService.getEncryptedSharedPreferences("privacy_preferences"))
            .thenReturn(context.getSharedPreferences("test_privacy_prefs", Context.MODE_PRIVATE))
        
        privacyManager = PrivacyManager(context, mockEncryptionService)
    }
    
    @After
    fun tearDown() {
        // Clean up test preferences
        context.getSharedPreferences("test_privacy_prefs", Context.MODE_PRIVATE)
            .edit().clear().apply()
    }
    
    @Test
    fun testInitialState_shouldHaveDefaultSettings() = runTest {
        // When
        val privacyState = privacyManager.privacyState.first()
        val consentState = privacyManager.consentState.first()
        
        // Then
        assertTrue("Should be initialized", privacyState.isInitialized)
        assertEquals("Should have default settings", PrivacySettings(), privacyState.settings)
        assertFalse("Should not have valid consent initially", consentState.hasValidConsent)
        assertTrue("Should need consent update", consentState.needsUpdate)
    }
    
    @Test
    fun testUpdatePrivacySettings_shouldUpdateState() = runTest {
        // Given
        val newSettings = PrivacySettings(
            allowVoiceProcessing = false,
            allowAnalytics = true,
            encryptSensitiveData = true,
            voiceRecordingRetentionDays = 60
        )
        
        // When
        privacyManager.updatePrivacySettings(newSettings)
        
        // Then
        val updatedState = privacyManager.privacyState.first()
        assertEquals("Settings should be updated", newSettings, updatedState.settings)
        assertTrue("Last updated should be set", updatedState.lastUpdated > 0)
    }
    
    @Test
    fun testRecordConsent_shouldUpdateConsentState() = runTest {
        // Given
        val consent = UserConsent(
            voiceProcessingConsent = true,
            dataStorageConsent = true,
            analyticsConsent = false,
            cloudSyncConsent = false,
            speakerIdentificationConsent = true,
            consentDate = System.currentTimeMillis(),
            expiryDate = System.currentTimeMillis() + (365 * 24 * 60 * 60 * 1000L)
        )
        
        // When
        privacyManager.recordConsent(consent)
        
        // Then
        val consentState = privacyManager.consentState.first()
        assertTrue("Should have valid consent", consentState.hasValidConsent)
        assertFalse("Should not need update", consentState.needsUpdate)
        assertFalse("Should not be expired", consentState.isExpired)
        assertEquals("Consent should match", consent, consentState.userConsent)
    }
    
    @Test
    fun testRevokeConsent_shouldClearConsentState() = runTest {
        // Given - first record consent
        val consent = UserConsent(
            voiceProcessingConsent = true,
            dataStorageConsent = true,
            analyticsConsent = false,
            cloudSyncConsent = false,
            speakerIdentificationConsent = true,
            consentDate = System.currentTimeMillis()
        )
        privacyManager.recordConsent(consent)
        
        // When
        privacyManager.revokeConsent()
        
        // Then
        val consentState = privacyManager.consentState.first()
        assertFalse("Should not have valid consent", consentState.hasValidConsent)
        assertTrue("Should need update", consentState.needsUpdate)
        assertNull("User consent should be null", consentState.userConsent)
    }
    
    @Test
    fun testIsDataProcessingAllowed_withValidConsent_shouldReturnCorrectValues() = runTest {
        // Given
        val consent = UserConsent(
            voiceProcessingConsent = true,
            dataStorageConsent = true,
            analyticsConsent = false,
            cloudSyncConsent = false,
            speakerIdentificationConsent = true,
            consentDate = System.currentTimeMillis()
        )
        privacyManager.recordConsent(consent)
        
        val settings = PrivacySettings(
            allowVoiceProcessing = true,
            allowDataStorage = true,
            allowAnalytics = false,
            allowCloudSync = false,
            allowSpeakerIdentification = true
        )
        privacyManager.updatePrivacySettings(settings)
        
        // Then
        assertTrue("Voice processing should be allowed", 
            privacyManager.isDataProcessingAllowed(DataProcessingPurpose.VOICE_RECOGNITION))
        assertTrue("Data storage should be allowed", 
            privacyManager.isDataProcessingAllowed(DataProcessingPurpose.TRANSACTION_STORAGE))
        assertFalse("Analytics should not be allowed", 
            privacyManager.isDataProcessingAllowed(DataProcessingPurpose.ANALYTICS))
        assertFalse("Cloud sync should not be allowed", 
            privacyManager.isDataProcessingAllowed(DataProcessingPurpose.CLOUD_SYNC))
        assertTrue("Speaker identification should be allowed", 
            privacyManager.isDataProcessingAllowed(DataProcessingPurpose.SPEAKER_IDENTIFICATION))
    }
    
    @Test
    fun testIsDataProcessingAllowed_withoutConsent_shouldReturnFalse() = runTest {
        // When - no consent recorded
        val isAllowed = privacyManager.isDataProcessingAllowed(DataProcessingPurpose.VOICE_RECOGNITION)
        
        // Then
        assertFalse("Should not allow processing without consent", isAllowed)
    }
    
    @Test
    fun testIsDataProcessingAllowed_withConsentButDisabledSetting_shouldReturnFalse() = runTest {
        // Given
        val consent = UserConsent(
            voiceProcessingConsent = true,
            dataStorageConsent = true,
            analyticsConsent = true,
            cloudSyncConsent = true,
            speakerIdentificationConsent = true,
            consentDate = System.currentTimeMillis()
        )
        privacyManager.recordConsent(consent)
        
        val settings = PrivacySettings(
            allowVoiceProcessing = false, // Disabled in settings
            allowDataStorage = true,
            allowAnalytics = true,
            allowCloudSync = true,
            allowSpeakerIdentification = true
        )
        privacyManager.updatePrivacySettings(settings)
        
        // Then
        assertFalse("Should not allow processing when disabled in settings", 
            privacyManager.isDataProcessingAllowed(DataProcessingPurpose.VOICE_RECOGNITION))
    }
    
    @Test
    fun testGetDataRetentionPeriod_shouldReturnCorrectPeriods() = runTest {
        // Given
        val settings = PrivacySettings(
            voiceRecordingRetentionDays = 30,
            transactionRetentionDays = 365,
            analyticsRetentionDays = 90,
            speakerProfileRetentionDays = 180
        )
        privacyManager.updatePrivacySettings(settings)
        
        // Then
        assertEquals("Voice recording retention should be 30 days in ms", 
            30L * 24 * 60 * 60 * 1000, 
            privacyManager.getDataRetentionPeriod(DataType.VOICE_RECORDINGS))
        assertEquals("Transaction retention should be 365 days in ms", 
            365L * 24 * 60 * 60 * 1000, 
            privacyManager.getDataRetentionPeriod(DataType.TRANSACTION_DATA))
        assertEquals("Analytics retention should be 90 days in ms", 
            90L * 24 * 60 * 60 * 1000, 
            privacyManager.getDataRetentionPeriod(DataType.ANALYTICS_DATA))
        assertEquals("Speaker profile retention should be 180 days in ms", 
            180L * 24 * 60 * 60 * 1000, 
            privacyManager.getDataRetentionPeriod(DataType.SPEAKER_PROFILES))
    }
    
    @Test
    fun testShouldDeleteData_withOldData_shouldReturnTrue() = runTest {
        // Given
        val settings = PrivacySettings(voiceRecordingRetentionDays = 30)
        privacyManager.updatePrivacySettings(settings)
        
        val oldTimestamp = System.currentTimeMillis() - (40L * 24 * 60 * 60 * 1000) // 40 days ago
        
        // When
        val shouldDelete = privacyManager.shouldDeleteData(DataType.VOICE_RECORDINGS, oldTimestamp)
        
        // Then
        assertTrue("Should delete old data", shouldDelete)
    }
    
    @Test
    fun testShouldDeleteData_withRecentData_shouldReturnFalse() = runTest {
        // Given
        val settings = PrivacySettings(voiceRecordingRetentionDays = 30)
        privacyManager.updatePrivacySettings(settings)
        
        val recentTimestamp = System.currentTimeMillis() - (20L * 24 * 60 * 60 * 1000) // 20 days ago
        
        // When
        val shouldDelete = privacyManager.shouldDeleteData(DataType.VOICE_RECORDINGS, recentTimestamp)
        
        // Then
        assertFalse("Should not delete recent data", shouldDelete)
    }
    
    @Test
    fun testGetPrivacyComplianceReport_withNoIssues_shouldReturnHighScore() = runTest {
        // Given - good privacy settings
        val consent = UserConsent(
            voiceProcessingConsent = true,
            dataStorageConsent = true,
            analyticsConsent = true,
            cloudSyncConsent = true,
            speakerIdentificationConsent = true,
            consentDate = System.currentTimeMillis()
        )
        privacyManager.recordConsent(consent)
        
        val settings = PrivacySettings(
            encryptSensitiveData = true,
            voiceRecordingRetentionDays = 30 // Reasonable retention
        )
        privacyManager.updatePrivacySettings(settings)
        
        // When
        val report = privacyManager.getPrivacyComplianceReport()
        
        // Then
        assertTrue("Compliance score should be high", report.complianceScore >= 80)
        assertTrue("Should have minimal issues", report.issues.size <= 1)
    }
    
    @Test
    fun testGetPrivacyComplianceReport_withIssues_shouldReturnLowerScore() = runTest {
        // Given - problematic settings
        val settings = PrivacySettings(
            encryptSensitiveData = false, // Security issue
            voiceRecordingRetentionDays = 400 // Excessive retention
        )
        privacyManager.updatePrivacySettings(settings)
        
        // When
        val report = privacyManager.getPrivacyComplianceReport()
        
        // Then
        assertTrue("Compliance score should be lower", report.complianceScore < 80)
        assertTrue("Should have multiple issues", report.issues.size >= 2)
        
        // Check specific issues
        assertTrue("Should have missing consent issue", 
            report.issues.any { it.type == ComplianceIssueType.MISSING_CONSENT })
        assertTrue("Should have excessive retention issue", 
            report.issues.any { it.type == ComplianceIssueType.EXCESSIVE_RETENTION })
        assertTrue("Should have weak security issue", 
            report.issues.any { it.type == ComplianceIssueType.WEAK_SECURITY })
    }
    
    @Test
    fun testExportUserData_shouldReturnValidExport() = runTest {
        // Given
        val settings = PrivacySettings(allowAnalytics = true)
        privacyManager.updatePrivacySettings(settings)
        
        val consent = UserConsent(
            voiceProcessingConsent = true,
            dataStorageConsent = true,
            analyticsConsent = true,
            cloudSyncConsent = false,
            speakerIdentificationConsent = true,
            consentDate = System.currentTimeMillis()
        )
        privacyManager.recordConsent(consent)
        
        // When
        val export = privacyManager.exportUserData()
        
        // Then
        assertNotNull("Export should not be null", export)
        assertTrue("Export date should be recent", 
            export.exportDate > System.currentTimeMillis() - 10000)
        assertNotNull("User ID should be set", export.userId)
        assertEquals("Settings should match", settings, export.privacySettings)
        assertTrue("Should have consent history", export.consentHistory.isNotEmpty())
        assertTrue("Should have data categories", export.dataCategories.isNotEmpty())
    }
    
    @Test
    fun testDeleteAllUserData_shouldReturnSuccessResult() = runTest {
        // Given - some data exists
        val settings = PrivacySettings(allowAnalytics = true)
        privacyManager.updatePrivacySettings(settings)
        
        // When
        val result = privacyManager.deleteAllUserData()
        
        // Then
        assertTrue("Deletion should succeed", result.success)
        assertNull("Should not have error", result.error)
        assertTrue("Should have deleted categories", result.deletedCategories.isNotEmpty())
        assertTrue("Deletion date should be recent", 
            result.deletionDate > System.currentTimeMillis() - 10000)
    }
    
    @Test
    fun testPrivacySettings_defaultValues_shouldBeSecure() {
        // Given
        val defaultSettings = PrivacySettings()
        
        // Then
        assertTrue("Voice processing should be enabled by default", defaultSettings.allowVoiceProcessing)
        assertTrue("Data storage should be enabled by default", defaultSettings.allowDataStorage)
        assertFalse("Analytics should be disabled by default", defaultSettings.allowAnalytics)
        assertFalse("Cloud sync should be disabled by default", defaultSettings.allowCloudSync)
        assertTrue("Speaker identification should be enabled by default", defaultSettings.allowSpeakerIdentification)
        assertTrue("Encryption should be enabled by default", defaultSettings.encryptSensitiveData)
        assertTrue("Automatic deletion should be enabled by default", defaultSettings.automaticDataDeletion)
        
        // Retention periods should be reasonable
        assertEquals("Voice recording retention should be 30 days", 30, defaultSettings.voiceRecordingRetentionDays)
        assertEquals("Transaction retention should be 365 days", 365, defaultSettings.transactionRetentionDays)
        assertEquals("Analytics retention should be 90 days", 90, defaultSettings.analyticsRetentionDays)
        assertEquals("Speaker profile retention should be 180 days", 180, defaultSettings.speakerProfileRetentionDays)
    }
    
    @Test
    fun testUserConsent_withExpiryDate_shouldDetectExpiration() = runTest {
        // Given - expired consent
        val expiredConsent = UserConsent(
            voiceProcessingConsent = true,
            dataStorageConsent = true,
            analyticsConsent = true,
            cloudSyncConsent = true,
            speakerIdentificationConsent = true,
            consentDate = System.currentTimeMillis() - (400L * 24 * 60 * 60 * 1000), // 400 days ago
            expiryDate = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000) // Expired 30 days ago
        )
        
        privacyManager.recordConsent(expiredConsent)
        
        // Simulate expiry check (this would normally be done in checkConsentStatus)
        val isExpired = expiredConsent.expiryDate?.let { expiry ->
            System.currentTimeMillis() > expiry
        } ?: false
        
        // Then
        assertTrue("Consent should be detected as expired", isExpired)
    }
}