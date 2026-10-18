package com.voiceledger.ghana.security

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** What the trader has agreed to. Defaults are the privacy-preserving ones. */
data class PrivacySettings(
    val allowAnalytics: Boolean = false,
    val allowCloudSync: Boolean = false,
    val allowSpeakerIdentification: Boolean = false,
    val encryptSensitiveData: Boolean = true,
    val deleteAudioAfterProcessing: Boolean = true,
    val audioRetentionDays: Int = 0,
    val transactionRetentionDays: Int = 365 * 2
)

/** One consent decision, with the date it was given and when it lapses. */
data class UserConsent(
    val voiceProcessingConsent: Boolean = false,
    val dataStorageConsent: Boolean = false,
    val analyticsConsent: Boolean = false,
    val cloudSyncConsent: Boolean = false,
    val speakerIdentificationConsent: Boolean = false,
    val consentDate: Long = 0L,
    val expiryDate: Long = 0L
)

data class ConsentState(
    val consent: UserConsent = UserConsent(),
    val hasValidConsent: Boolean = false,
    val isExpired: Boolean = false,
    val needsUpdate: Boolean = true
)

data class PrivacyState(
    val settings: PrivacySettings = PrivacySettings()
)

enum class DataType { AUDIO, TRANSCRIPT, TRANSACTION, SPEAKER_PROFILE, ANALYTICS }

enum class DataProcessingPurpose {
    VOICE_PROCESSING, SPEAKER_IDENTIFICATION, ANALYTICS, CLOUD_SYNC, LEDGER_STORAGE
}

data class PrivacyComplianceReport(
    val complianceScore: Int = 0,
    val hasValidConsent: Boolean = false,
    val issues: List<String> = emptyList(),
    val generatedAt: Long = System.currentTimeMillis()
)

data class DataExportResult(
    val success: Boolean = false,
    val payload: String = "",
    val message: String? = null
)

data class DataDeletionResult(
    val success: Boolean = false,
    val message: String? = null
)

/**
 * Consent and retention policy for the trader's data.
 *
 * Settings and consent are persisted in encrypted preferences and exposed as state
 * so the UI reflects them immediately. Every default denies: a trader who has not
 * answered yet is treated as having refused, so no processing happens by accident.
 *
 * INCOMPLETE: [exportUserData] and [deleteAllUserData] record the request and report
 * failure rather than pretending to act. Both must reach the Room database and the
 * audio store before this ships — they are the Data Protection Act commitments the
 * site makes to traders. PrivacyManagerTest (395 lines) is the specification.
 */
@Singleton
class PrivacyManager @Inject constructor(
    private val context: Context,
    private val encryptionService: EncryptionService
) {

    companion object {
        private const val PREFS_NAME = "privacy_settings"
        private const val KEY_ANALYTICS = "allow_analytics"
        private const val KEY_CLOUD_SYNC = "allow_cloud_sync"
        private const val KEY_SPEAKER_ID = "allow_speaker_identification"
        private const val KEY_ENCRYPT = "encrypt_sensitive_data"
        private const val KEY_CONSENT_DATE = "consent_date"
        private const val KEY_CONSENT_EXPIRY = "consent_expiry"
        private const val KEY_VOICE_CONSENT = "voice_processing_consent"
        private const val KEY_STORAGE_CONSENT = "data_storage_consent"

        private const val DAY_MS = 24 * 60 * 60 * 1000L
    }

    private val prefs by lazy { encryptionService.getEncryptedSharedPreferences(PREFS_NAME) }

    private val _privacyState = MutableStateFlow(PrivacyState())
    val privacyState: StateFlow<PrivacyState> = _privacyState.asStateFlow()

    private val _consentState = MutableStateFlow(ConsentState())
    val consentState: StateFlow<ConsentState> = _consentState.asStateFlow()

    init {
        _privacyState.value = PrivacyState(readSettings())
        _consentState.value = toConsentState(readConsent())
    }

    fun updatePrivacySettings(settings: PrivacySettings) {
        prefs.edit()
            .putBoolean(KEY_ANALYTICS, settings.allowAnalytics)
            .putBoolean(KEY_CLOUD_SYNC, settings.allowCloudSync)
            .putBoolean(KEY_SPEAKER_ID, settings.allowSpeakerIdentification)
            .putBoolean(KEY_ENCRYPT, settings.encryptSensitiveData)
            .apply()
        _privacyState.value = PrivacyState(settings)
    }

    fun recordConsent(consent: UserConsent) {
        prefs.edit()
            .putBoolean(KEY_VOICE_CONSENT, consent.voiceProcessingConsent)
            .putBoolean(KEY_STORAGE_CONSENT, consent.dataStorageConsent)
            .putLong(KEY_CONSENT_DATE, consent.consentDate)
            .putLong(KEY_CONSENT_EXPIRY, consent.expiryDate)
            .apply()
        _consentState.value = toConsentState(consent)
    }

    fun revokeConsent() {
        prefs.edit()
            .remove(KEY_VOICE_CONSENT)
            .remove(KEY_STORAGE_CONSENT)
            .remove(KEY_CONSENT_DATE)
            .remove(KEY_CONSENT_EXPIRY)
            .apply()
        _consentState.value = ConsentState()
    }

    /** A purpose is allowed only when consent is valid and the matching setting is on. */
    fun isDataProcessingAllowed(purpose: DataProcessingPurpose): Boolean {
        val consent = _consentState.value
        if (!consent.hasValidConsent) return false
        val settings = _privacyState.value.settings
        return when (purpose) {
            DataProcessingPurpose.VOICE_PROCESSING -> consent.consent.voiceProcessingConsent
            DataProcessingPurpose.LEDGER_STORAGE -> consent.consent.dataStorageConsent
            DataProcessingPurpose.ANALYTICS -> settings.allowAnalytics
            DataProcessingPurpose.CLOUD_SYNC -> settings.allowCloudSync
            DataProcessingPurpose.SPEAKER_IDENTIFICATION -> settings.allowSpeakerIdentification
        }
    }

    /** Retention window in milliseconds. Audio defaults to zero: delete after processing. */
    fun getDataRetentionPeriod(dataType: DataType): Long {
        val settings = _privacyState.value.settings
        return when (dataType) {
            DataType.AUDIO -> settings.audioRetentionDays * DAY_MS
            DataType.TRANSCRIPT, DataType.TRANSACTION -> settings.transactionRetentionDays * DAY_MS
            DataType.SPEAKER_PROFILE -> settings.transactionRetentionDays * DAY_MS
            DataType.ANALYTICS -> 90 * DAY_MS
        }
    }

    fun shouldDeleteData(dataType: DataType, creationTime: Long): Boolean {
        val retention = getDataRetentionPeriod(dataType)
        if (retention <= 0L) return true
        return System.currentTimeMillis() - creationTime > retention
    }

    fun getPrivacyComplianceReport(): PrivacyComplianceReport {
        val consent = _consentState.value
        val settings = _privacyState.value.settings
        val issues = buildList {
            if (!consent.hasValidConsent) add("No valid consent on record")
            if (consent.isExpired) add("Consent has expired and must be renewed")
            if (!settings.encryptSensitiveData) add("Sensitive field encryption is switched off")
            if (!settings.deleteAudioAfterProcessing) add("Audio is being retained after processing")
        }
        val score = (100 - issues.size * 25).coerceAtLeast(0)
        return PrivacyComplianceReport(
            complianceScore = score,
            hasValidConsent = consent.hasValidConsent,
            issues = issues
        )
    }

    /**
     * INCOMPLETE: must gather the trader's transactions, summaries and speaker profile
     * into a portable file. Reports failure rather than returning an empty export that
     * could be mistaken for "you have no data".
     */
    fun exportUserData(): DataExportResult = DataExportResult(
        success = false,
        message = "Data export is not implemented yet"
    )

    /**
     * INCOMPLETE: must erase the Room database, audio files and preferences. Reports
     * failure rather than telling a trader her data is gone when it is not.
     */
    fun deleteAllUserData(): DataDeletionResult = DataDeletionResult(
        success = false,
        message = "Data deletion is not implemented yet"
    )

    private fun readSettings() = PrivacySettings(
        allowAnalytics = prefs.getBoolean(KEY_ANALYTICS, false),
        allowCloudSync = prefs.getBoolean(KEY_CLOUD_SYNC, false),
        allowSpeakerIdentification = prefs.getBoolean(KEY_SPEAKER_ID, false),
        encryptSensitiveData = prefs.getBoolean(KEY_ENCRYPT, true)
    )

    private fun readConsent() = UserConsent(
        voiceProcessingConsent = prefs.getBoolean(KEY_VOICE_CONSENT, false),
        dataStorageConsent = prefs.getBoolean(KEY_STORAGE_CONSENT, false),
        consentDate = prefs.getLong(KEY_CONSENT_DATE, 0L),
        expiryDate = prefs.getLong(KEY_CONSENT_EXPIRY, 0L)
    )

    private fun toConsentState(consent: UserConsent): ConsentState {
        val given = consent.consentDate > 0L
        val expired = given && consent.expiryDate in 1..System.currentTimeMillis()
        return ConsentState(
            consent = consent,
            hasValidConsent = given && !expired && consent.voiceProcessingConsent,
            isExpired = expired,
            needsUpdate = !given || expired
        )
    }
}
