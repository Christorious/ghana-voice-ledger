package com.voiceledger.ghana.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@AndroidEntryPoint
class VoiceAgentService : Service() {
    
    companion object {
        const val ACTION_START_LISTENING = "com.voiceledger.ghana.START_LISTENING"
        const val ACTION_PAUSE_LISTENING = "com.voiceledger.ghana.PAUSE_LISTENING"
        const val ACTION_STOP_LISTENING = "com.voiceledger.ghana.STOP_LISTENING"
    }
    
    @Inject
    lateinit var sessionCoordinator: VoiceSessionCoordinator
    
    @Inject
    lateinit var notificationHelper: VoiceNotificationHelper
    
    private val binder = VoiceAgentBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var initializationJob: Job? = null
    
    private var audioCallback: AudioProcessingCallback? = null
    
    val listeningState: StateFlow<ListeningState>
        get() = sessionCoordinator.listeningState
    
    var audioProcessingCallback: AudioProcessingCallback?
        get() = audioCallback
        set(value) {
            audioCallback = value
            sessionCoordinator.setAudioProcessingCallback(value)
        }
    
    override fun onCreate() {
        super.onCreate()
        notificationHelper.createNotificationChannel()
        
        initializationJob = serviceScope.launch {
            sessionCoordinator.initialize()
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_LISTENING -> startListening()
            ACTION_PAUSE_LISTENING -> pauseListening()
            ACTION_STOP_LISTENING -> stopListening()
        }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onDestroy() {
        super.onDestroy()
        sessionCoordinator.cleanup()
        serviceScope.cancel()
    }
    
    fun startListening() {
        serviceScope.launch {
            initializationJob?.join()
            val started = sessionCoordinator.startListening()
            if (started) {
                val notification = sessionCoordinator.getForegroundNotification()
                startForeground(VoiceNotificationHelper.NOTIFICATION_ID, notification)
            }
        }
    }
    
    fun pauseListening() {
        sessionCoordinator.pauseListening()
    }
    
    fun stopListening() {
        sessionCoordinator.stopListening()
        stopForeground(true)
        stopSelf()
    }
    
    fun getServiceStats(): ServiceStats {
        return sessionCoordinator.getServiceStats()
    }
    
    fun setAudioProcessingEnabled(enabled: Boolean) {
    }
    
    fun setBackgroundSyncEnabled(enabled: Boolean) {
    }
    
    fun setProcessingInterval(intervalMs: Long) {
    }
    
    fun setVADSensitivity(sensitivity: Float) {
    }
    
    fun setAudioBufferSize(bufferSize: Int) {
    }
    
    fun getPowerOptimizationSettings(): PowerOptimizationSettings {
        return PowerOptimizationSettings(
            audioProcessingEnabled = true,
            backgroundSyncEnabled = true,
            processingIntervalMs = 1000,
            vadSensitivity = 0.5f,
            audioBufferSize = 2048
        )
    }
    
    inner class VoiceAgentBinder : Binder() {
        fun getService(): VoiceAgentService = this@VoiceAgentService
    }
}

data class PowerOptimizationSettings(
    val audioProcessingEnabled: Boolean = true,
    val backgroundSyncEnabled: Boolean = true,
    val processingIntervalMs: Long = 1000,
    val vadSensitivity: Float = 0.5f,
    val audioBufferSize: Int = 2048
)
