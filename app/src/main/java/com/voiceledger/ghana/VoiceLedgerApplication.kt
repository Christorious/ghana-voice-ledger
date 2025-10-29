package com.voiceledger.ghana

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import com.voiceledger.ghana.security.SecurityManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class VoiceLedgerApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var securityManager: SecurityManager

    override fun onCreate() {
        super.onCreate()
        
        runBlocking(Dispatchers.IO) {
            try {
                securityManager.ensureDatabaseKey()
            } catch (e: SecurityException) {
                Timber.e(e, "Database encryption key unavailable")
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error while initializing database encryption key")
                throw SecurityException("Failed to initialize database encryption key", e)
            }
        }
        
        // Initialize App Center (for analytics and crash reporting)
        initializeAppCenter()
        
        // Initialize logging
        initializeLogging()
        
        // Create notification channels
        createNotificationChannels()
        
        // Initialize WorkManager
        initializeWorkManager()
        
        Timber.i("VoiceLedgerApplication initialized successfully")
    }
    
    private fun initializeAppCenter() {
        // App Center will be configured when you set up the app in App Center portal
        // The app secret will be automatically added by App Center
        // For now, we'll initialize it without a secret (it will be added later)
        if (!AppCenter.isConfigured()) {
            AppCenter.start(
                this,
                BuildConfig.APP_CENTER_SECRET,
                Analytics::class.java,
                Crashes::class.java
            )
            Timber.i("App Center initialized")
        }
    }
    
    private fun initializeLogging() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            // Plant a custom tree for release builds that doesn't log debug/verbose
            Timber.plant(object : Timber.Tree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                    if (priority >= android.util.Log.INFO) {
                        // Log to crash reporting service in production
                        // FirebaseCrashlytics.getInstance().log("$tag: $message")
                        // t?.let { FirebaseCrashlytics.getInstance().recordException(it) }
                    }
                }
            })
        }
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Voice Agent Service Channel
            val voiceAgentChannel = NotificationChannel(
                VOICE_AGENT_CHANNEL_ID,
                "Voice Agent Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for voice recording and processing"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            
            // Transaction Notifications Channel
            val transactionChannel = NotificationChannel(
                TRANSACTION_CHANNEL_ID,
                "Transaction Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for transaction confirmations and summaries"
                setShowBadge(true)
                enableVibration(true)
            }
            
            // System Notifications Channel
            val systemChannel = NotificationChannel(
                SYSTEM_CHANNEL_ID,
                "System Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Important system notifications and alerts"
                setShowBadge(true)
                enableVibration(true)
            }
            
            // Daily Summary Channel
            val summaryChannel = NotificationChannel(
                SUMMARY_CHANNEL_ID,
                "Daily Summaries",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily transaction summaries and insights"
                setShowBadge(true)
                enableVibration(false)
            }
            
            notificationManager.createNotificationChannels(listOf(
                voiceAgentChannel,
                transactionChannel,
                systemChannel,
                summaryChannel
            ))
            
            Timber.d("Notification channels created")
        }
    }
    
    private fun initializeWorkManager() {
        try {
            WorkManager.initialize(this, workManagerConfiguration)
            Timber.d("WorkManager initialized")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize WorkManager")
        }
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.INFO)
            .build()
    
    override fun onLowMemory() {
        super.onLowMemory()
        Timber.w("Low memory warning received")
    }
    
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Timber.d("Memory trim requested: level $level")
    }
    
    companion object {
        const val VOICE_AGENT_CHANNEL_ID = "voice_agent_service"
        const val TRANSACTION_CHANNEL_ID = "transaction_notifications"
        const val SYSTEM_CHANNEL_ID = "system_notifications"
        const val SUMMARY_CHANNEL_ID = "daily_summaries"
    }
}