package com.voiceledger.ghana.service

import android.content.Context
import android.os.PowerManager as AndroidPowerManager
import com.voiceledger.ghana.ml.speech.SpeechRecognitionManager
import com.voiceledger.ghana.ml.vad.SleepMode
import com.voiceledger.ghana.ml.vad.VADManager
import com.voiceledger.ghana.offline.NetworkUtils
import com.voiceledger.ghana.offline.OfflineQueueManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.Calendar
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceSessionCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioCaptureController: AudioCaptureController,
    private val speechProcessingPipeline: SpeechProcessingPipeline,
    private val notificationHelper: VoiceNotificationHelper,
    private val vadManager: VADManager,
    private val speechRecognitionManager: SpeechRecognitionManager,
    private val offlineQueueManager: OfflineQueueManager,
    private val powerManager: com.voiceledger.ghana.service.PowerManager,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    companion object {
        private const val SLEEP_TIMEOUT_MS = 30000L
        private const val MARKET_OPEN_HOUR = 6
        private const val MARKET_CLOSE_HOUR = 18
        private const val WAKE_LOCK_TAG = "VoiceAgent:AudioRecording"
    }

    private val sessionJob = SupervisorJob()
    private val sessionScope = CoroutineScope(defaultDispatcher + sessionJob)
    
    private val _listeningState = MutableStateFlow<ListeningState>(ListeningState.STOPPED)
    val listeningState: StateFlow<ListeningState> = _listeningState.asStateFlow()
    
    private val isPaused = AtomicBoolean(false)
    private val isInSleepMode = AtomicBoolean(false)
    
    private var lastActivityTime = System.currentTimeMillis()
    private var totalChunksProcessed = 0
    private var speechChunksDetected = 0
    
    private var audioProcessingCallback: AudioProcessingCallback? = null
    private var wakeLock: AndroidPowerManager.WakeLock? = null
    
    private var processingJob: Job? = null

    suspend fun initialize() {
        speechProcessingPipeline.initialize()
        NetworkUtils.initialize(context)
        
        sessionScope.launch {
            vadManager.sleepModeChanges.collect { sleepMode ->
                handleSleepModeChange(sleepMode)
            }
        }
        
        sessionScope.launch {
            powerManager.powerState.collect { powerState ->
                handlePowerStateChange(powerState)
            }
        }
        
        sessionScope.launch {
            NetworkUtils.networkState.collect { networkState ->
                handleNetworkStateChange(networkState)
            }
        }
    }

    fun setAudioProcessingCallback(callback: AudioProcessingCallback?) {
        this.audioProcessingCallback = callback
    }

    fun startListening(): Boolean {
        if (_listeningState.value is ListeningState.LISTENING) return false
        
        if (!audioCaptureController.checkAudioPermission()) {
            _listeningState.value = ListeningState.ERROR("Audio permission not granted")
            return false
        }
        
        if (!isMarketHours()) {
            _listeningState.value = ListeningState.ERROR("Outside market hours (6 AM - 6 PM)")
            return false
        }
        
        return try {
            acquireWakeLock()
            audioCaptureController.initialize()
            speechProcessingPipeline.startProcessing()
            
            startAudioProcessing(delayMs = 10)
            
            _listeningState.value = ListeningState.LISTENING
            isPaused.set(false)
            isInSleepMode.set(false)
            
            true
        } catch (e: Exception) {
            _listeningState.value = ListeningState.ERROR("Failed to start: ${e.message}")
            false
        }
    }

    fun pauseListening() {
        if (_listeningState.value !is ListeningState.LISTENING) return
        
        isPaused.set(true)
        _listeningState.value = ListeningState.PAUSED
        
        processingJob?.cancel()
        audioCaptureController.stopRecording()
    }

    fun stopListening() {
        isPaused.set(false)
        isInSleepMode.set(false)
        
        speechProcessingPipeline.stopProcessing()
        processingJob?.cancel()
        audioCaptureController.release()
        
        _listeningState.value = ListeningState.STOPPED
        releaseWakeLock()
    }

    fun cleanup() {
        stopListening()
        speechProcessingPipeline.cleanup()
        powerManager.cleanup()
        offlineQueueManager.cleanup()
        NetworkUtils.cleanup()
        sessionScope.cancel()
    }

    fun getServiceStats(): ServiceStats {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
        val batteryLevel = batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
        
        return ServiceStats(
            totalChunksProcessed = totalChunksProcessed,
            speechChunksDetected = speechChunksDetected,
            currentState = _listeningState.value,
            isInSleepMode = isInSleepMode.get(),
            lastActivityTime = lastActivityTime,
            batteryLevel = batteryLevel
        )
    }

    private fun startAudioProcessing(delayMs: Long) {
        audioCaptureController.startRecording(sessionScope, delayMs)
        
        processingJob = sessionScope.launch {
            launch {
                audioCaptureController.audioChunks.collect { audioChunk ->
                    if (audioChunk.error != null) {
                        _listeningState.value = ListeningState.ERROR("Recording error: ${audioChunk.error.message}")
                        return@collect
                    }
                    
                    totalChunksProcessed++
                    
                    val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
                    val batteryLevel = batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
                    
                    speechProcessingPipeline.submitChunk(
                        audioChunk = audioChunk,
                        batteryLevel = batteryLevel,
                        powerSavingMode = isInSleepMode.get(),
                        callback = audioProcessingCallback
                    )
                }
            }
            
            launch {
                speechProcessingPipeline.results.collect { result ->
                    if (result is ProcessingResult.Success && result.hasActivity) {
                        lastActivityTime = result.timestamp
                        speechChunksDetected++
                    }
                    
                    if (shouldEnterSleepMode()) {
                        enterSleepMode()
                    }
                }
            }
        }
    }

    private fun enterSleepMode() {
        if (isInSleepMode.get()) return
        
        isInSleepMode.set(true)
        _listeningState.value = ListeningState.SLEEPING
        
        processingJob?.cancel()
        audioCaptureController.stopRecording()
        
        startAudioProcessing(delayMs = 500)
        
        notificationHelper.updateNotification(notificationHelper.createSleepingNotification())
    }

    private fun wakeFromSleep() {
        if (!isInSleepMode.get()) return
        
        isInSleepMode.set(false)
        _listeningState.value = ListeningState.LISTENING
        lastActivityTime = System.currentTimeMillis()
        
        processingJob?.cancel()
        audioCaptureController.stopRecording()
        
        startAudioProcessing(delayMs = 10)
        
        notificationHelper.updateNotification(notificationHelper.createListeningNotification())
    }

    private fun handleSleepModeChange(sleepMode: SleepMode) {
        when (sleepMode) {
            SleepMode.AWAKE -> {
                if (isInSleepMode.get()) {
                    wakeFromSleep()
                }
            }
            SleepMode.LIGHT_SLEEP, SleepMode.DEEP_SLEEP -> {
                if (!isInSleepMode.get()) {
                    enterSleepMode()
                }
            }
        }
    }

    private fun handlePowerStateChange(powerState: PowerState) {
        when (powerState.powerMode) {
            PowerMode.NORMAL -> {
                if (isPaused.get() && audioCaptureController.isActive()) {
                    resumeFromPowerSave()
                }
            }
            PowerMode.POWER_SAVE -> {
                vadManager.setConfiguration(
                    adaptiveMode = true,
                    batteryOptimization = true
                )
            }
            PowerMode.CRITICAL_SAVE -> {
                vadManager.setConfiguration(
                    adaptiveMode = true,
                    batteryOptimization = true
                )
                
                if (!isPaused.get()) {
                    processingJob?.cancel()
                    audioCaptureController.stopRecording()
                    startAudioProcessing(delayMs = 2000)
                }
            }
            PowerMode.SLEEP -> {
                if (_listeningState.value is ListeningState.LISTENING) {
                    pauseListening()
                    _listeningState.value = ListeningState.SLEEPING
                }
            }
        }
    }

    private fun resumeFromPowerSave() {
        if (isPaused.get() && audioCaptureController.isActive()) {
            isPaused.set(false)
            _listeningState.value = ListeningState.LISTENING
            startAudioProcessing(delayMs = 10)
        }
    }

    private fun handleNetworkStateChange(networkState: com.voiceledger.ghana.offline.NetworkState) {
        sessionScope.launch {
            if (networkState.isAvailable) {
                offlineQueueManager.processAllPendingOperations()
                speechRecognitionManager.setPreferOnlineRecognition(true)
                
                if (_listeningState.value is ListeningState.LISTENING) {
                    notificationHelper.updateNotification(notificationHelper.createListeningNotification())
                }
            } else {
                speechRecognitionManager.setPreferOnlineRecognition(false)
                
                if (_listeningState.value is ListeningState.LISTENING) {
                    notificationHelper.updateNotification(notificationHelper.createOfflineNotification())
                }
            }
        }
    }

    private fun shouldEnterSleepMode(): Boolean {
        val timeSinceLastActivity = System.currentTimeMillis() - lastActivityTime
        return timeSinceLastActivity > SLEEP_TIMEOUT_MS && !isInSleepMode.get()
    }

    private fun isMarketHours(): Boolean {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        return currentHour in MARKET_OPEN_HOUR until MARKET_CLOSE_HOUR
    }

    private fun acquireWakeLock() {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as AndroidPowerManager
        wakeLock = powerManager.newWakeLock(
            AndroidPowerManager.PARTIAL_WAKE_LOCK,
            WAKE_LOCK_TAG
        ).apply {
            acquire(10 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }

    fun getForegroundNotification(): android.app.Notification {
        return notificationHelper.createListeningNotification()
    }
}

sealed class ListeningState {
    object STOPPED : ListeningState()
    object LISTENING : ListeningState()
    object PAUSED : ListeningState()
    object SLEEPING : ListeningState()
    data class ERROR(val message: String) : ListeningState()
}

interface AudioProcessingCallback {
    suspend fun onAudioChunkProcessed(
        audioData: ShortArray, 
        chunkId: String, 
        timestamp: Long,
        speakerResult: com.voiceledger.ghana.ml.speaker.SpeakerResult? = null
    )
}

data class ServiceStats(
    val totalChunksProcessed: Int,
    val speechChunksDetected: Int,
    val currentState: ListeningState,
    val isInSleepMode: Boolean,
    val lastActivityTime: Long,
    val batteryLevel: Int
)
