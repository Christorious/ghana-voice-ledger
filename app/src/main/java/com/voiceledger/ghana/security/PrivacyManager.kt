package com.voiceledger.ghana.security

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** What the trader has agreed to. Defaults are the privacy-preserving ones. */
data class PrivacySettings(
    val allowVoiceProcessing: Boolean = false,
    val allowDataStorage: Boolean = false,
    val allowAnalytics: Boolean = false,
    val allowCloudSync: Boolean = false,
    val allowSpeakerIdentification: Boolean = false,
    val encryptSensitiveData: Boolean = true,
    /** Delete data as soon as its retention window closes. Defaults on. */
    val automaticDataDeletion: Boolean = true,
    val voiceRecordingRetentionDays: Int = 0,
    val transactionRetentionDays: Int = 365,
    val analyticsRetentionDays: Int = 90,
    val speakerProfileRetentionDays: Int = 180
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
    val userConsent: UserConsent = UserConsent(),
    val hasValidConsent: Boolean = false,
    val isExpired: Boolean = false,
    val needsUpdate: Boolean = true
)

data class PrivacyState(
    val settings: PrivacySettings = PrivacySettings(),
    val isInitialized: Boolean = false,
    val lastUpdated: Long = 0L
)

/**
 * The categories SecureDataStorage deletes by, and the trader can set retention on.
 * These names are the contract: SecureDataStorage.deleteDataOlderThan branches on them.
 */
enum class DataType { VOICE_RECORDINGS, TRANSACTION_DATA, ANALYTICS_DATA, SPEAKER_PROFILES }

enum class DataProcessingPurpose {
    VOICE_RECOGNITION, TRANSACTION_STORAGE, ANALYTICS, CLOUD_SYNC, SPEAKER_IDENTIFICATION
}

enum class ComplianceIssueType { MISSING_CONSENT, EXPIRED_CONSENT, EXCESSIVE_RETENTION, WEAK_SECURITY }

data class ComplianceIssue(
    val type: ComplianceIssueType,
    val description: String
)

data class PrivacyComplianceReport(
    val complianceScore: Int = 0,
    val hasValidConsent: Boolean = false,
    val issues: List<ComplianceIssue> = emptyList(),
    /** What to do about [issues], in the trader's terms. Shown under the score. */
    val recommendations: List<String> = emptyList(),
    val generatedAt: Long = System.currentTimeMillis()
)

/** Everything the trader is entitled to a copy of under the Data Protection Act. */
data class UserDataExport(
    val userId: String,
    val exportDate: Long,
    val privacySettings: PrivacySettings,
    val consentHistory: List<UserConsent>
)

data class DataDeletionResult(
    val success: Boolean = false,
    val deletedCategories: List<DataType> = emptyList(),
    val deletionDate: Long = System.currentTimeMillis(),
    val error: String? = null
)

/**
 * Consent and retention policy for the trader's data.
 *
 * Settings and consent are persisted in encrypted preferences and exposed as state
 * so the UI reflects them immediately. Every default denies: a trader who has not
 * answered yet is treated as having refused, so no processing happens by accident.
 *
 * INCOMPLETE: [deleteAllUserData] records the request and reports failure rather
 * than pretending to act — it must reach the Room database and the audio store
 * before this ships. [exportUserData] returns the settings and consent this class
 * actually holds; the transactions and summaries still have to be added to it.
 * PrivacyManagerTest (395 lines) is the specification, and its deletion test is
 * expected to fail until the deletion is real.
 */
@Singleton
class PrivacyManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryptionService: EncryptionService
) {

    companion object {
        private const val PREFS_NAME = "privacy_settings"
        private const val KEY_VOICE_PROCESSING = "allow_voice_processing"
        private const val KEY_DATA_STORAGE = "allow_data_storage"
        private const val KEY_ANALYTICS = "allow_analytics"
        private const val KEY_CLOUD_SYNC = "allow_cloud_sync"
        private const val KEY_SPEAKER_ID = "allow_speaker_identification"
        private const val KEY_ENCRYPT = "encrypt_sensitive_data"
        private const val KEY_AUTO_DELETE = "automatic_data_deletion"
        private const val KEY_RETAIN_VOICE = "voice_recording_retention_days"
        private const val KEY_RETAIN_TRANSACTION = "transaction_retention_days"
        private const val KEY_RETAIN_ANALYTICS = "analytics_retention_days"
        private const val KEY_RETAIN_SPEAKER = "speaker_profile_retention_days"
        private const val KEY_CONSENT_DATE = "consent_date"
        private const val KEY_CONSENT_EXPIRY = "consent_expiry"
        private const val KEY_VOICE_CONSENT = "voice_processing_consent"
        private const val KEY_STORAGE_CONSENT = "data_storage_consent"
        private const val KEY_ANALYTICS_CONSENT = "analytics_consent"
        private const val KEY_CLOUD_CONSENT = "cloud_sync_consent"
        private const val KEY_SPEAKER_CONSENT = "speaker_identification_consent"
        private const val KEY_USER_ID = "user_id"

        private const val DAY_MS = 24L * 60 * 60 * 1000

        /** Beyond this, keeping voice recordings is hard to justify to a trader. */
        private const val MAX_REASONABLE_VOICE_RETENTION_DAYS = 90
    }

    private val prefs by lazy { encryptionService.getEncryptedSharedPreferences(PREFS_NAME) }

    private val _privacyState = MutableStateFlow(PrivacyState())
    val privacyState: StateFlow<PrivacyState> = _privacyState.asStateFlow()

    private val _consentState = MutableStateFlow(ConsentState())
    val consentState: StateFlow<ConsentState> = _consentState.asStateFlow()

    init {
        _privacyState.value = PrivacyState(
            settings = readSettings(),
            isInitialized = true,
            lastUpdated = System.currentTimeMillis()
        )
        _consentState.value = toConsentState(readConsent())
    }

    fun updatePrivacySettings(settings: PrivacySettings) {
        prefs.edit()
            .putBoolean(KEY_VOICE_PROCESSING, settings.allowVoiceProcessing)
            .putBoolean(KEY_DATA_STORAGE, settings.allowDataStorage)
            .putBoolean(KEY_ANALYTICS, settings.allowAnalytics)
            .putBoolean(KEY_CLOUD_SYNC, settings.allowCloudSync)
            .putBoolean(KEY_SPEAKER_ID, settings.allowSpeakerIdentification)
            .putBoolean(KEY_ENCRYPT, settings.encryptSensitiveData)
            .putBoolean(KEY_AUTO_DELETE, settings.automaticDataDeletion)
            .putInt(KEY_RETAIN_VOICE, settings.voiceRecordingRetentionDays)
            .putInt(KEY_RETAIN_TRANSACTION, settings.transactionRetentionDays)
            .putInt(KEY_RETAIN_ANALYTICS, settings.analyticsRetentionDays)
            .putInt(KEY_RETAIN_SPEAKER, settings.speakerProfileRetentionDays)
            .apply()
        _privacyState.value = PrivacyState(
            settings = settings,
            isInitialized = true,
            lastUpdated = System.currentTimeMillis()
        )
    }

    fun recordConsent(consent: UserConsent) {
        prefs.edit()
            .putBoolean(KEY_VOICE_CONSENT, consent.voiceProcessingConsent)
            .putBoolean(KEY_STORAGE_CONSENT, consent.dataStorageConsent)
            .putBoolean(KEY_ANALYTICS_CONSENT, consent.analyticsConsent)
            .putBoolean(KEY_CLOUD_CONSENT, consent.cloudSyncConsent)
            .putBoolean(KEY_SPEAKER_CONSENT, consent.speakerIdentificationConsent)
            .putLong(KEY_CONSENT_DATE, consent.consentDate)
            .putLong(KEY_CONSENT_EXPIRY, consent.expiryDate)
            .apply()
        _consentState.value = toConsentState(consent)
    }

    fun revokeConsent() {
        prefs.edit()
            .remove(KEY_VOICE_CONSENT)
            .remove(KEY_STORAGE_CONSENT)
            .remove(KEY_ANALYTICS_CONSENT)
            .remove(KEY_CLOUD_CONSENT)
            .remove(KEY_SPEAKER_CONSENT)
            .remove(KEY_CONSENT_DATE)
            .remove(KEY_CONSENT_EXPIRY)
            .apply()
        _consentState.value = ConsentState()
    }

    /** A purpose is allowed only when consent is valid and the matching setting is on. */
    fun isDataProcessingAllowed(purpose: DataProcessingPurpose): Boolean {
        val state = _consentState.value
        if (!state.hasValidConsent) return false
        val consent = state.userConsent
        val settings = _privacyState.value.settings
        return when (purpose) {
            DataProcessingPurpose.VOICE_RECOGNITION ->
                consent.voiceProcessingConsent && settings.allowVoiceProcessing
            DataProcessingPurpose.TRANSACTION_STORAGE ->
                consent.dataStorageConsent && settings.allowDataStorage
            DataProcessingPurpose.ANALYTICS ->
                consent.analyticsConsent && settings.allowAnalytics
            DataProcessingPurpose.CLOUD_SYNC ->
                consent.cloudSyncConsent && settings.allowCloudSync
            DataProcessingPurpose.SPEAKER_IDENTIFICATION ->
                consent.speakerIdentificationConsent && settings.allowSpeakerIdentification
        }
    }

    /** Retention window in milliseconds. Voice defaults to zero: delete after processing. */
    fun getDataRetentionPeriod(dataType: DataType): Long {
        val settings = _privacyState.value.settings
        val days = when (dataType) {
            DataType.VOICE_RECORDINGS -> settings.voiceRecordingRetentionDays
            DataType.TRANSACTION_DATA -> settings.transactionRetentionDays
            DataType.ANALYTICS_DATA -> settings.analyticsRetentionDays
            DataType.SPEAKER_PROFILES -> settings.speakerProfileRetentionDays
        }
        return days * DAY_MS
    }

    fun shouldDeleteData(dataType: DataType, creationTime: Long): Boolean {
        val retention = getDataRetentionPeriod(dataType)
        if (retention <= 0L) return true
        return System.currentTimeMillis() - creationTime > retention
    }

    fun getPrivacyComplianceReport(): PrivacyComplianceReport {
        val state = _consentState.value
        val settings = _privacyState.value.settings
        val issues = buildList {
            if (!state.hasValidConsent) add(
                ComplianceIssue(ComplianceIssueType.MISSING_CONSENT, "No valid consent on record")
            )
            if (state.isExpired) add(
                ComplianceIssue(ComplianceIssueType.EXPIRED_CONSENT, "Consent has expired and must be renewed")
            )
            if (!settings.encryptSensitiveData) add(
                ComplianceIssue(ComplianceIssueType.WEAK_SECURITY, "Sensitive field encryption is switched off")
            )
            if (settings.voiceRecordingRetentionDays > MAX_REASONABLE_VOICE_RETENTION_DAYS) add(
                ComplianceIssue(
                    ComplianceIssueType.EXCESSIVE_RETENTION,
                    "Voice recordings are kept for ${settings.voiceRecordingRetentionDays} days"
                )
            )
        }
        val recommendations = issues.map { issue ->
            when (issue.type) {
                ComplianceIssueType.MISSING_CONSENT -> "Ask the trader to review and accept the consent screen"
                ComplianceIssueType.EXPIRED_CONSENT -> "Consent has lapsed — ask for it again"
                ComplianceIssueType.WEAK_SECURITY -> "Turn encryption back on for sensitive fields"
                ComplianceIssueType.EXCESSIVE_RETENTION ->
                    "Shorten voice recording retention to $MAX_REASONABLE_VOICE_RETENTION_DAYS days or fewer"
            }
        }
        return PrivacyComplianceReport(
            complianceScore = (100 - issues.size * 25).coerceAtLeast(0),
            hasValidConsent = state.hasValidConsent,
            issues = issues,
            recommendations = recommendations
        )
    }

    /**
     * The settings and consent this class holds, with a stable per-install id.
     *
     * INCOMPLETE: the trader's transactions, summaries and speaker profile are not in
     * here yet. What it does return is real, not a placeholder.
     */
    fun exportUserData(): UserDataExport {
        val consent = _consentState.value.userConsent
        return UserDataExport(
            userId = userId(),
            exportDate = System.currentTimeMillis(),
            privacySettings = _privacyState.value.settings,
            consentHistory = if (consent.consentDate > 0L) listOf(consent) else emptyList()
        )
    }

    /**
     * INCOMPLETE: must erase the Room database, audio files and preferences. Reports
     * failure rather than telling a trader her data is gone when it is not.
     */
    fun deleteAllUserData(): DataDeletionResult = DataDeletionResult(
        success = false,
        deletedCategories = emptyList(),
        error = "Data deletion is not implemented yet"
    )

    /** A random id minted on first export; it identifies the install, not the person. */
    private fun userId(): String =
        prefs.getString(KEY_USER_ID, null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString(KEY_USER_ID, it).apply()
        }

    private fun readSettings(): PrivacySettings {
        val defaults = PrivacySettings()
        return PrivacySettings(
            allowVoiceProcessing = prefs.getBoolean(KEY_VOICE_PROCESSING, defaults.allowVoiceProcessing),
            allowDataStorage = prefs.getBoolean(KEY_DATA_STORAGE, defaults.allowDataStorage),
            allowAnalytics = prefs.getBoolean(KEY_ANALYTICS, defaults.allowAnalytics),
            allowCloudSync = prefs.getBoolean(KEY_CLOUD_SYNC, defaults.allowCloudSync),
            allowSpeakerIdentification =
                prefs.getBoolean(KEY_SPEAKER_ID, defaults.allowSpeakerIdentification),
            encryptSensitiveData = prefs.getBoolean(KEY_ENCRYPT, defaults.encryptSensitiveData),
            automaticDataDeletion = prefs.getBoolean(KEY_AUTO_DELETE, defaults.automaticDataDeletion),
            voiceRecordingRetentionDays =
                prefs.getInt(KEY_RETAIN_VOICE, defaults.voiceRecordingRetentionDays),
            transactionRetentionDays =
                prefs.getInt(KEY_RETAIN_TRANSACTION, defaults.transactionRetentionDays),
            analyticsRetentionDays =
                prefs.getInt(KEY_RETAIN_ANALYTICS, defaults.analyticsRetentionDays),
            speakerProfileRetentionDays =
                prefs.getInt(KEY_RETAIN_SPEAKER, defaults.speakerProfileRetentionDays)
        )
    }

    private fun readConsent() = UserConsent(
        voiceProcessingConsent = prefs.getBoolean(KEY_VOICE_CONSENT, false),
        dataStorageConsent = prefs.getBoolean(KEY_STORAGE_CONSENT, false),
        analyticsConsent = prefs.getBoolean(KEY_ANALYTICS_CONSENT, false),
        cloudSyncConsent = prefs.getBoolean(KEY_CLOUD_CONSENT, false),
        speakerIdentificationConsent = prefs.getBoolean(KEY_SPEAKER_CONSENT, false),
        consentDate = prefs.getLong(KEY_CONSENT_DATE, 0L),
        expiryDate = prefs.getLong(KEY_CONSENT_EXPIRY, 0L)
    )

    private fun toConsentState(consent: UserConsent): ConsentState {
        val given = consent.consentDate > 0L
        val expired = given && consent.expiryDate in 1..System.currentTimeMillis()
        return ConsentState(
            userConsent = consent,
            hasValidConsent = given && !expired && consent.voiceProcessingConsent,
            isExpired = expired,
            needsUpdate = !given || expired
        )
    }
}
