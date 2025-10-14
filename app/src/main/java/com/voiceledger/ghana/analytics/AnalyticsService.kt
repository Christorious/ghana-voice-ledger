package com.voiceledger.ghana.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Analytics service for tracking user interactions and app performance
 * Uses Firebase Analytics with custom events for Ghana Voice Ledger
 */
@Singleton
class AnalyticsService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val firebaseAnalytics: FirebaseAnalytics by lazy {
        Firebase.analytics
    }
    
    private val analyticsScope = CoroutineScope(Dispatchers.IO)
    
    companion object {
        // Custom event names
        const val EVENT_TRANSACTION_CREATED = "transaction_created"
        const val EVENT_VOICE_RECOGNITION_STARTED = "voice_recognition_started"
        const val EVENT_VOICE_RECOGNITION_COMPLETED = "voice_recognition_completed"
        const val EVENT_SPEAKER_IDENTIFIED = "speaker_identified"
        const val EVENT_LANGUAGE_DETECTED = "language_detected"
        const val EVENT_OFFLINE_TRANSACTION = "offline_transaction"
        const val EVENT_SYNC_COMPLETED = "sync_completed"
        const val EVENT_ERROR_OCCURRED = "error_occurred"
        const val EVENT_DAILY_SUMMARY_GENERATED = "daily_summary_generated"
        const val EVENT_SETTINGS_CHANGED = "settings_changed"
        const val EVENT_PRIVACY_CONSENT_GIVEN = "privacy_consent_given"
        const val EVENT_DATA_EXPORT_REQUESTED = "data_export_requested"
        const val EVENT_BATTERY_OPTIMIZATION_TRIGGERED = "battery_optimization_triggered"
        const val EVENT_PERFORMANCE_ISSUE_DETECTED = "performance_issue_detected"
        
        // Parameter names
        const val PARAM_PRODUCT_NAME = "product_name"
        const val PARAM_TRANSACTION_AMOUNT = "transaction_amount"
        const val PARAM_QUANTITY = "quantity"
        const val PARAM_CONFIDENCE_SCORE = "confidence_score"
        const val PARAM_LANGUAGE = "language"
        const val PARAM_SPEAKER_ID = "speaker_id"
        const val PARAM_ERROR_TYPE = "error_type"
        const val PARAM_ERROR_MESSAGE = "error_message"
        const val PARAM_SETTING_NAME = "setting_name"
        const val PARAM_SETTING_VALUE = "setting_value"
        const val PARAM_BATTERY_LEVEL = "battery_level"
        const val PARAM_MEMORY_USAGE = "memory_usage"
        const val PARAM_CPU_USAGE = "cpu_usage"
        const val PARAM_PROCESSING_TIME = "processing_time"
        const val PARAM_NETWORK_STATUS = "network_status"
        const val PARAM_SYNC_DURATION = "sync_duration"
        const val PARAM_ITEMS_SYNCED = "items_synced"
    }
    
    /**
     * Initialize analytics service
     */
    fun initialize() {
        analyticsScope.launch {
            // Set default user properties
            setUserProperty("app_version", getAppVersion())
            setUserProperty("device_language", getDeviceLanguage())
            setUserProperty("country", "Ghana")
            
            // Track app start
            logEvent("app_started", Bundle().apply {
                putString("session_id", generateSessionId())
                putLong("timestamp", System.currentTimeMillis())
            })
        }
    }
    
    /**
     * Log transaction creation event
     */
    fun logTransactionCreated(
        productName: String,
        amount: Double,
        quantity: Int,
        confidenceScore: Float,
        speakerId: String?,
        language: String?
    ) {
        analyticsScope.launch {
            val bundle = Bundle().apply {
                putString(PARAM_PRODUCT_NAME, productName)
                putDouble(PARAM_TRANSACTION_AMOUNT, amount)
                putInt(PARAM_QUANTITY, quantity)
                putFloat(PARAM_CONFIDENCE_SCORE, confidenceScore)
                speakerId?.let { putString(PARAM_SPEAKER_ID, it) }
                language?.let { putString(PARAM_LANGUAGE, it) }
            }
            
            logEvent(EVENT_TRANSACTION_CREATED, bundle)
            
            // Also log as Firebase's built-in purchase event for e-commerce tracking
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.PURCHASE, Bundle().apply {
                putString(FirebaseAnalytics.Param.CURRENCY, "GHS")
                putDouble(FirebaseAnalytics.Param.VALUE, amount)
                putString(FirebaseAnalytics.Param.ITEM_NAME, productName)
                putInt(FirebaseAnalytics.Param.QUANTITY, quantity)
            })
        }
    }
    
    /**
     * Log voice recognition events
     */
    fun logVoiceRecognitionStarted(language: String?) {
        analyticsScope.launch {
            val bundle = Bundle().apply {
                language?.let { putString(PARAM_LANGUAGE, it) }
                putLong("start_time", System.currentTimeMillis())
            }
            logEvent(EVENT_VOICE_RECOGNITION_STARTED, bundle)
        }
    }
    
    fun logVoiceRecognitionCompleted(
        language: String?,
        confidenceScore: Float,
        processingTimeMs: Long,
        success: Boolean
    ) {
        analyticsScope.launch {
            val bundle = Bundle().apply {
                language?.let { putString(PARAM_LANGUAGE, it) }
                putFloat(PARAM_CONFIDENCE_SCORE, confidenceScore)
                putLong(PARAM_PROCESSING_TIME, processingTimeMs)
                putBoolean("success", success)
            }
            logEvent(EVENT_VOICE_RECOGNITION_COMPLETED, bundle)
        }
    }
    
    /**
     * Log speaker identification events
     */
    fun logSpeakerIdentified(speakerId: String, confidenceScore: Float) {
        analyticsScope.launch {
            val bundle = Bundle().apply {
                putString(PARAM_SPEAKER_ID, speakerId)
                putFloat(PARAM_CONFIDENCE_SCORE, confidenceScore)
            }
            logEvent(EVENT_SPEAKER_IDENTIFIED, bundle)
        }
    }
    
    /**
     * Log language detection events
     */
    fun logLanguageDetected(language: String, confidenceScore: Float) {
        analyticsScope.launch {
            val bundle = Bundle().apply {
                putString(PARAM_LANGUAGE, language)
                putFloat(PARAM_CONFIDENCE_SCORE, confidenceScore)
            }
            logEvent(EVENT_LANGUAGE_DETECTED, bundle)
        }
    }
    
    /**
     * Log offline functionality events
     */
    fun logOfflineTransaction(productName: String, amount: Double) {
        analyticsScope.launch {
            val bundle = Bundle().apply {
                putString(PARAM_PRODUCT_NAME, productName)
                putDouble(PARAM_TRANSACTION_AMOUNT, amount)
                putString(PARAM_NETWORK_STATUS, "offline")
            }
            logEvent(EVENT_OFFLINE_TRANSACTION, bundle)
        }
    }
    
    fun logSyncCompleted(itemsSynced: Int, syncDurationMs: Long, success: Boolean) {
        analyticsScope.launch {
            val bundle = Bundle().apply {
                putInt(PARAM_ITEMS_SYNCED, itemsSynced)
                putLong(PARAM_SYNC_DURATION, syncDurationMs)
                putBoolean("success", success)
            }
            logEvent(EVENT_SYNC_COMPLETED, bundle)
        }
    }
    
    /**
     * Log error events
     */
    fun logError(errorType: String, errorMessage: String, context: String? = null) {
        analyticsScope.launch {
            val bundle = Bundle().apply {
                putString(PARAM_ERROR_TYPE, errorType)
                putString(PARAM_ERROR_MESSAGE, errorMessage.take(100)) // Limit message length
                context?.let { putString("error_context", it) }
            }
            logEvent(EVENT_ERROR_OCCURRED, bundle)
        }
    }
    
    /**
     * Log daily summary events
     */
    fun logDailySummaryGenerated(
        totalSales: Double,
        transactionCount: Int,
        topProduct: String?,
        generationTimeMs: Long
    ) {
        analyticsScope.launch {
            
   /**
     * Enable/disable analytics collection
     */
    fun setAnalyticsCollectionEnabled(enabled: Boolean) {
        firebaseAnalytics.setAnalyticsCollectionEnabled(enabled)
    }
    
    /**
     * Get app version
     */
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
    
    /**
     * Get device language
     */
    private fun getDeviceLanguage(): String {
        return java.util.Locale.getDefault().language
    }
    
    /**
     * Generate session ID
     */
    private fun generateSessionId(): String {
        return "${System.currentTimeMillis()}_${(Math.random() * 10000).toInt()}"
    }
}