package com.voiceledger.ghana.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.voiceledger.ghana.R
import com.voiceledger.ghana.domain.repository.AudioMetadataRepository
import com.voiceledger.ghana.data.local.entity.AudioMetadata
import com.voiceledger.ghana.ml.vad.VADManager
import com.voiceledger.ghana.ml.vad.VADType
import com.voiceledger.ghana.ml.vad.SleepMode
import com.voiceledger.ghana.ml.speaker.SpeakerIdentifier
import com.voiceledger.ghana.ml.speaker.SpeakerType
import com.voiceledger.ghana.ml.speaker.SpeakerResult
import com.voiceledger.ghana.ml.transaction.TransactionProcessor
import com.voiceledger.ghana.ml.speech.SpeechRecognitionManager
import com.voiceledger.ghana.ml.speech.TranscriptionResult
import com.voiceledger.ghana.offline.OfflineQueueManager
import com.voiceledger.ghana.offline.NetworkUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.math.sqrt

/**
 * # VoiceAgentService
 *
 * A long-running foreground service that orchestrates all voice-driven interactions in the
 * Ghana Voice Ledger application. The service manages continuous audio capture, speech
 * activity detection, speaker identification, transcription, transaction understanding, and
 * power/network heuristics to provide hands-free bookkeeping for market vendors.
 *
 * ## Service State Diagram
 *
 * ```
 * ┌────────────┐       startListening()        ┌──────────────┐
 * │  STOPPED   │──────────────────────────────▶│  LISTENING   │
 * └────────────┘                               └──────┬───────┘
 *       ▲                                            pauseListening()
 *       │ stopListening()                             │
 *       │                                             ▼
 *       │                                     ┌──────────────┐
 *       │                                     │   PAUSED     │
 *       │                                     └──────────────┘
 *       │                                             │ (sleep timeout /
 *       │                                             │ low energy)
 *       │                                             ▼
 *       │                                     ┌──────────────┐
 *       │                                     │  SLEEPING    │
 *       │                                     └──────────────┘
 *       │                                             │ (speech activity /
 *       │                                             │ manual resume)
 *       │                                             ▼
 *       └─────────────────────────────────────────────┘
 *
 * ERROR is treated as an overlay state that can be emitted from any state when an unrecoverable
 * issue occurs (for example missing microphone permission or recorder failure).
 * ```
 *
 * ## Audio Processing Pipeline
 *
 * ```
 * Microphone
 *    │ 16 kHz PCM frames
 *    ▼
 * [AudioRecord]
 *    │
 *    ▼
 * [Voice Activity Detection (VAD)] ───► Sleep mode manager
 *    │ Speech frames
 *    ▼
 * [SpeakerIdentifier]
 *    │ Annotated frames
 *    ▼
 * [SpeechRecognitionManager]
 *    │ Transcripts + confidence
 *    ▼
 * [TransactionProcessor]
 *    │ Transaction states + persisted metadata
 *    ▼
 * [OfflineQueueManager] (when network is unavailable)
 * ```
 *
 * ## Responsibilities
 *
 * - Maintains a foreground notification to comply with Android background execution limits.
 * - Manages wake locks, sleep mode heuristics, and battery-aware throttling for all voice
 *   processing components.
 * - Coordinates machine-learning modules (VAD, speaker ID, speech recognition, transaction
 *   understanding) and streams resulting metadata to the persistence layer.
 * - Keeps track of listening state via a coroutine-driven state flow for UI components.
 * - Integrates with [OfflineQueueManager] to defer network-dependent tasks when offline.
 * - Emits callbacks to presentation layers whenever audio chunks with speech are processed.
 *
 * ## Threading Model
 *
 * All long-running work is executed inside `serviceScope`, a `Dispatchers.Default` coroutine
 * scope. Audio capture happens on the same scope to guarantee serialized access to the
 * `AudioRecord` instance, while downstream ML modules can schedule their own coroutines.
 *
 * ## Related Components
 * - [VADManager]: Detects speech activity and sleep mode triggers.
 * - [SpeakerIdentifier]: Distinguishes sellers from customers for contextual processing.
 * - [SpeechRecognitionManager]: Produces transcripts optimized for market environments.
 * - [TransactionProcessor]: Converts conversational context into structured transactions.
 * - [OfflineQueueManager]: Stores metadata and sync jobs until connectivity returns.
 */
@AndroidEntryPoint
class VoiceAgentService : Service() {
    
    companion object {
        const val ACTION_START_LISTENING = "com.voiceledger.ghana.START_LISTENING"
        const val ACTION_PAUSE_LISTENING = "com.voiceledger.ghana.PAUSE_LISTENING"
        const val ACTION_STOP_LISTENING = "com.voiceledger.ghana.STOP_LISTENING"
        
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "voice_agent_channel"
        private const val CHANNEL_NAME = "Voice Agent Service"
        
        // Audio configuration optimized for speech recognition
        private const val SAMPLE_RATE = 16000 // 16kHz for speech recognition
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val AUDIO_SOURCE = MediaRecorder.AudioSource.MIC
        
        // Buffer configuration for 1-second chunks
        private const val BUFFER_SIZE_MULTIPLIER = 4
        private const val CHUNK_DURATION_MS = 1000L // 1 second chunks
        private const val SAMPLES_PER_CHUNK = SAMPLE_RATE // 16000 samples per second
        
        // Power management
        private const val SILENCE_THRESHOLD = 0.01 // RMS threshold for silence detection
        private const val SLEEP_TIMEOUT_MS = 30000L // 30 seconds of silence before sleep
        private const val WAKE_LOCK_TAG = "VoiceAgent:AudioRecording"
        
        // Market hours (6 AM - 6 PM)
        private const val MARKET_OPEN_HOUR = 6
        private const val MARKET_CLOSE_HOUR = 18
    }
    
    @Inject
    lateinit var audioMetadataRepository: AudioMetadataRepository
    
    @Inject
    lateinit var vadManager: VADManager
    
    @Inject
    lateinit var speakerIdentifier: SpeakerIdentifier
    
    @Inject
    lateinit var transactionProcessor: TransactionProcessor
    
    @Inject
    lateinit var speechRecognitionManager: SpeechRecognitionManager
    
    @Inject
    lateinit var powerManager: PowerManager
    
    @Inject
    lateinit var offlineQueueManager: OfflineQueueManager
    
    private val binder = VoiceAgentBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Audio recording components
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    
    // State management
    private val _listeningState = MutableStateFlow(ListeningState.STOPPED)
    val listeningState: StateFlow<ListeningState> = _listeningState.asStateFlow()
    
    private val isRecording = AtomicBoolean(false)
    private val isPaused = AtomicBoolean(false)
    private val isInSleepMode = AtomicBoolean(false)
    
    // Performance tracking
    private var lastActivityTime = System.currentTimeMillis()
    private var totalChunksProcessed = 0
    private var speechChunksDetected = 0
    
    // Audio processing callback
    var audioProcessingCallback: AudioProcessingCallback? = null
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
        
        // Initialize all ML components
        serviceScope.launch {
            vadManager.initialize(VADType.CUSTOM)
            speakerIdentifier.initialize()
            speechRecognitionManager.optimizeForMarketEnvironment()
            
            // Initialize network monitoring
            NetworkUtils.initialize(this@VoiceAgentService)
            
            // Listen to sleep mode changes
            vadManager.sleepModeChanges.collect { sleepMode ->
                handleSleepModeChange(sleepMode)
            }
        }
        
        // Monitor power state for battery optimization
        serviceScope.launch {
            powerManager.powerState.collect { powerState ->
                handlePowerStateChange(powerState)
            }
        }
        
        // Monitor network state for offline functionality
        serviceScope.launch {
            NetworkUtils.networkState.collect { networkState ->
                handleNetworkStateChange(networkState)
            }
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_LISTENING -> startListening()
            ACTION_PAUSE_LISTENING -> pauseListening()
            ACTION_STOP_LISTENING -> stopListening()
        }
        return START_STICKY // Restart service if killed
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onDestroy() {
        super.onDestroy()
        stopListening()
        vadManager.destroy()
        speakerIdentifier.cleanup()
        speechRecognitionManager.cleanup()
        transactionProcessor.cleanup()
        powerManager.cleanup()
        offlineQueueManager.cleanup()
        NetworkUtils.cleanup()
        releaseWakeLock()
        serviceScope.cancel()
    }
    
    /**
     * Start continuous audio listening
     */
    fun startListening() {
        if (isRecording.get()) return
        
        if (!checkAudioPermission()) {
            _listeningState.value = ListeningState.ERROR("Audio permission not granted")
            return
        }
        
        if (!isMarketHours()) {
            _listeningState.value = ListeningState.ERROR("Outside market hours (6 AM - 6 PM)")
            return
        }
        
        try {
            initializeAudioRecord()
            startForegroundService()
            
            // Start VAD processing
            vadManager.startProcessing()
            
            startRecordingLoop()
            
            _listeningState.value = ListeningState.LISTENING
            isPaused.set(false)
            isInSleepMode.set(false)
            
        } catch (e: Exception) {
            _listeningState.value = ListeningState.ERROR("Failed to start recording: ${e.message}")
        }
    }
    
    /**
     * Pause audio listening
     */
    fun pauseListening() {
        if (!isRecording.get()) return
        
        isPaused.set(true)
        _listeningState.value = ListeningState.PAUSED
        
        // Keep service running but stop processing
        recordingJob?.cancel()
    }
    
    /**
     * Stop audio listening and service
     */
    fun stopListening() {
        isRecording.set(false)
        isPaused.set(false)
        isInSleepMode.set(false)
        
        // Stop VAD processing
        vadManager.stopProcessing()
        
        recordingJob?.cancel()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        
        _listeningState.value = ListeningState.STOPPED
        stopForeground(true)
        stopSelf()
    }
    
    /**
     * Enter smart sleep mode during inactivity
     */
    private fun enterSleepMode() {
        if (isInSleepMode.get()) return
        
        isInSleepMode.set(true)
        _listeningState.value = ListeningState.SLEEPING
        
        // Reduce processing frequency during sleep
        recordingJob?.cancel()
        startSleepModeRecording()
    }
    
    /**
     * Wake up from sleep mode
     */
    private fun wakeFromSleep() {
        if (!isInSleepMode.get()) return
        
        isInSleepMode.set(false)
        _listeningState.value = ListeningState.LISTENING
        lastActivityTime = System.currentTimeMillis()
        
        // Resume normal recording
        recordingJob?.cancel()
        startRecordingLoop()
    }
    
    /**
     * Initialize AudioRecord with optimal settings for speech recognition
     */
    private fun initializeAudioRecord() {
        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        ) * BUFFER_SIZE_MULTIPLIER
        
        audioRecord = AudioRecord(
            AUDIO_SOURCE,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize
        ).apply {
            if (state != AudioRecord.STATE_INITIALIZED) {
                throw IllegalStateException("AudioRecord initialization failed")
            }
        }
    }
    
    /**
     * Start the main recording loop
     */
    private fun startRecordingLoop() {
        recordingJob = serviceScope.launch {
            audioRecord?.startRecording()
            isRecording.set(true)
            
            val buffer = ShortArray(SAMPLES_PER_CHUNK)
            
            while (isRecording.get() && !isPaused.get()) {
                try {
                    val samplesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    
                    if (samplesRead > 0) {
                        processAudioChunk(buffer, samplesRead)
                        
                        // Check for sleep mode
                        if (shouldEnterSleepMode()) {
                            enterSleepMode()
                            break
                        }
                    }
                    
                    // Small delay to prevent excessive CPU usage
                    delay(10)
                    
                } catch (e: Exception) {
                    _listeningState.value = ListeningState.ERROR("Recording error: ${e.message}")
                    break
                }
            }
        }
    }
    
    /**
     * Start reduced frequency recording during sleep mode
     */
    private fun startSleepModeRecording() {
        recordingJob = serviceScope.launch {
            val buffer = ShortArray(SAMPLES_PER_CHUNK)
            
            while (isRecording.get() && isInSleepMode.get()) {
                try {
                    val samplesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    
                    if (samplesRead > 0) {
                        val hasActivity = detectActivity(buffer, samplesRead)
                        
                        if (hasActivity) {
                            vadManager.forceWakeUp()
                            break
                        }
                    }
                    
                    // Longer delay during sleep mode to save battery
                    delay(500)
                    
                } catch (e: Exception) {
                    _listeningState.value = ListeningState.ERROR("Sleep mode error: ${e.message}")
                    break
                }
            }
        }
    }
    
    /**
     * Process audio chunk and extract metadata
     */
    private suspend fun processAudioChunk(buffer: ShortArray, samplesRead: Int) {
        val chunkId = "chunk_${System.currentTimeMillis()}_${UUID.randomUUID()}"
        val timestamp = System.currentTimeMillis()
        val processingStartTime = System.currentTimeMillis()
        
        try {
            // Convert to byte array for VAD processing
            val audioBytes = convertShortsToBytes(buffer, samplesRead)
            
            // Process with VAD Manager
            val vadResult = vadManager.processAudioSample(audioBytes)
            
            val hasActivity = vadResult.isSpeech && vadResult.confidence > 0.3f
            
            // Speaker identification and speech recognition for speech segments
            var speakerResult: SpeakerResult? = null
            var transcriptionResult: com.voiceledger.ghana.ml.speech.TranscriptionResult? = null
            
            if (hasActivity) {
                // Identify speaker
                speakerResult = speakerIdentifier.identifySpeaker(audioBytes)
                
                // Transcribe speech
                transcriptionResult = speechRecognitionManager.transcribe(audioBytes)
                
                // Process transaction if we have a good transcription
                if (transcriptionResult.isSuccess && transcriptionResult.transcript.isNotBlank()) {
                    val isSeller = speakerResult?.speakerType == SpeakerType.SELLER
                    val speakerId = speakerResult?.speakerId ?: "unknown"
                    
                    transactionProcessor.processUtterance(
                        transcript = transcriptionResult.transcript,
                        speakerId = speakerId,
                        isSeller = isSeller,
                        confidence = transcriptionResult.confidence,
                        timestamp = timestamp,
                        audioChunkId = chunkId
                    )
                }
                
                lastActivityTime = timestamp
                speechChunksDetected++
            }
            
            totalChunksProcessed++
            
            // Create audio metadata
            val metadata = AudioMetadata(
                chunkId = chunkId,
                timestamp = timestamp,
                vadScore = vadResult.confidence,
                speechDetected = hasActivity,
                speakerDetected = speakerResult != null && speakerResult.speakerType != SpeakerType.UNKNOWN,
                speakerId = speakerResult?.speakerId,
                speakerConfidence = speakerResult?.confidence,
                audioQuality = vadResult.energyLevel,
                durationMs = CHUNK_DURATION_MS,
                processingTimeMs = System.currentTimeMillis() - processingStartTime,
                contributedToTransaction = false,
                transactionId = null,
                errorMessage = null,
                batteryLevel = getBatteryLevel(),
                powerSavingMode = vadManager.shouldUsePowerSavingMode()
            )
            
            // Store metadata
            audioMetadataRepository.insertMetadata(metadata)
            
            // Pass to audio processing callback if available
            if (hasActivity) {
                audioProcessingCallback?.onAudioChunkProcessed(
                    buffer.copyOf(samplesRead),
                    chunkId,
                    timestamp,
                    speakerResult
                )
            }
            
        } catch (e: Exception) {
            // Log error metadata
            val errorMetadata = AudioMetadata(
                chunkId = chunkId,
                timestamp = timestamp,
                vadScore = 0f,
                speechDetected = false,
                speakerDetected = false,
                speakerId = null,
                speakerConfidence = null,
                audioQuality = null,
                durationMs = CHUNK_DURATION_MS,
                processingTimeMs = System.currentTimeMillis() - processingStartTime,
                contributedToTransaction = false,
                transactionId = null,
                errorMessage = e.message,
                batteryLevel = getBatteryLevel(),
                powerSavingMode = isInSleepMode.get()
            )
            
            audioMetadataRepository.insertMetadata(errorMetadata)
        }
    }
    
    /**
     * Calculate RMS (Root Mean Square) for audio activity detection
     */
    private fun calculateRMS(buffer: ShortArray, length: Int): Double {
        var sum = 0.0
        for (i in 0 until length) {
            sum += buffer[i] * buffer[i]
        }
        return sqrt(sum / length) / Short.MAX_VALUE
    }
    
    /**
     * Detect audio activity for sleep mode wake-up
     */
    private suspend fun detectActivity(buffer: ShortArray, length: Int): Boolean {
        val audioBytes = convertShortsToBytes(buffer, length)
        val vadResult = vadManager.processAudioSample(audioBytes)
        return vadResult.isSpeech && vadResult.confidence > 0.3f
    }
    
    /**
     * Convert short array to byte array for VAD processing
     */
    private fun convertShortsToBytes(buffer: ShortArray, length: Int): ByteArray {
        val bytes = ByteArray(length * 2)
        for (i in 0 until length) {
            val sample = buffer[i]
            bytes[i * 2] = (sample.toInt() and 0xFF).toByte()
            bytes[i * 2 + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
        }
        return bytes
    }
    
    /**
     * Handle sleep mode changes from VAD Manager
     */
    private fun handleSleepModeChange(sleepMode: SleepMode) {
        when (sleepMode) {
            SleepMode.AWAKE -> {
                if (isInSleepMode.get()) {
                    wakeFromSleep()
                }
            }
            SleepMode.LIGHT_SLEEP -> {
                if (!isInSleepMode.get()) {
                    enterSleepMode()
                }
            }
            SleepMode.DEEP_SLEEP -> {
                if (!isInSleepMode.get()) {
                    enterSleepMode()
                }
            }
        }
    }
    
    /**
     * Handle power state changes from Power Manager
     */
    private fun handlePowerStateChange(powerState: PowerState) {
        when (powerState.powerMode) {
            PowerMode.NORMAL -> {
                // Resume normal operation if paused due to power saving
                if (isPaused.get() && isRecording.get()) {
                    resumeFromPowerSave()
                }
            }
            PowerMode.POWER_SAVE -> {
                // Reduce processing frequency but keep running
                adjustProcessingForPowerSave()
            }
            PowerMode.CRITICAL_SAVE -> {
                // Minimal processing only
                adjustProcessingForCriticalSave()
            }
            PowerMode.SLEEP -> {
                // Market closed - pause service
                if (isRecording.get()) {
                    pauseForMarketClosure()
                }
            }
        }
    }
    
    /**
     * Resume from power save mode
     */
    private fun resumeFromPowerSave() {
        if (isPaused.get() && isRecording.get()) {
            isPaused.set(false)
            _listeningState.value = ListeningState.LISTENING
            startRecordingLoop()
        }
    }
    
    /**
     * Adjust processing for power save mode
     */
    private fun adjustProcessingForPowerSave() {
        // Reduce VAD sensitivity to save CPU
        vadManager.setConfiguration(
            adaptiveMode = true,
            batteryOptimization = true
        )
    }
    
    /**
     * Adjust processing for critical save mode
     */
    private fun adjustProcessingForCriticalSave() {
        // Further reduce processing
        vadManager.setConfiguration(
            adaptiveMode = true,
            batteryOptimization = true
        )
        
        // Use longer processing intervals
        recordingJob?.cancel()
        if (isRecording.get() && !isPaused.get()) {
            startCriticalSaveRecording()
        }
    }
    
    /**
     * Pause service for market closure
     */
    private fun pauseForMarketClosure() {
        pauseListening()
        _listeningState.value = ListeningState.SLEEPING
    }
    
    /**
     * Start recording with critical save optimizations
     */
    private fun startCriticalSaveRecording() {
        recordingJob = serviceScope.launch {
            audioRecord?.startRecording()
            isRecording.set(true)
            
            val buffer = ShortArray(SAMPLES_PER_CHUNK)
            
            while (isRecording.get() && !isPaused.get()) {
                try {
                    val samplesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    
                    if (samplesRead > 0) {
                        processAudioChunk(buffer, samplesRead)
                    }
                    
                    // Longer delay for critical save mode
                    delay(2000) // 2 seconds instead of 10ms
                    
                } catch (e: Exception) {
                    _listeningState.value = ListeningState.ERROR("Recording error: ${e.message}")
                    break
                }
            }
        }
    }
    
    /**
     * Handle network state changes for offline functionality
     */
    private fun handleNetworkStateChange(networkState: com.voiceledger.ghana.offline.NetworkState) {
        serviceScope.launch {
            if (networkState.isAvailable) {
                // Network is back - process pending offline operations
                offlineQueueManager.processAllPendingOperations()
                
                // Update speech recognition to use online mode if preferred
                speechRecognitionManager.setPreferOnlineRecognition(true)
            } else {
                // Network lost - ensure offline mode
                speechRecognitionManager.setPreferOnlineRecognition(false)
                
                // Update listening state to show offline status
                if (_listeningState.value is ListeningState.LISTENING) {
                    updateNotificationForOfflineMode()
                }
            }
        }
    }
    
    /**
     * Update notification to show offline status
     */
    private fun updateNotificationForOfflineMode() {
        val notification = createNotification().apply {
            // Update notification to show offline status
        }
        startForeground(NOTIFICATION_ID, notification)
    }
    
    /**
     * Check if service should enter sleep mode
     */
    private fun shouldEnterSleepMode(): Boolean {
        val timeSinceLastActivity = System.currentTimeMillis() - lastActivityTime
        return timeSinceLastActivity > SLEEP_TIMEOUT_MS && !isInSleepMode.get()
    }
    
    /**
     * Check if current time is within market hours
     */
    private fun isMarketHours(): Boolean {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        return currentHour in MARKET_OPEN_HOUR until MARKET_CLOSE_HOUR
    }
    
    /**
     * Check audio recording permission
     */
    private fun checkAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Get current battery level
     */
    private fun getBatteryLevel(): Int {
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
        return batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }
    
    /**
     * Create notification channel for Android O+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Voice Agent background service"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Start foreground service with notification
     */
    private fun startForegroundService() {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
    }
    
    /**
     * Create service notification
     */
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val pauseIntent = Intent(this, VoiceAgentService::class.java).apply {
            action = ACTION_PAUSE_LISTENING
        }
        val pausePendingIntent = PendingIntent.getService(
            this, 1, pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val stopIntent = Intent(this, VoiceAgentService::class.java).apply {
            action = ACTION_STOP_LISTENING
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 2, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Voice Ledger Active")
            .setContentText("Listening for transactions...")
            .setSmallIcon(R.drawable.ic_mic)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_pause, "Pause", pausePendingIntent)
            .addAction(R.drawable.ic_stop, "Stop", stopPendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
    
    /**
     * Acquire wake lock to keep CPU active during recording
     */
    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            WAKE_LOCK_TAG
        ).apply {
            acquire(10 * 60 * 1000L) // 10 minutes timeout
        }
    }
    
    /**
     * Release wake lock
     */
    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }
    
    /**
     * Get service statistics
     */
    fun getServiceStats(): ServiceStats {
        return ServiceStats(
            totalChunksProcessed = totalChunksProcessed,
            speechChunksDetected = speechChunksDetected,
            currentState = _listeningState.value,
            isInSleepMode = isInSleepMode.get(),
            lastActivityTime = lastActivityTime,
            batteryLevel = getBatteryLevel()
        )
    }
    
    // Power Optimization Methods
    
    /**
     * Enable or disable audio processing
     */
    fun setAudioProcessingEnabled(enabled: Boolean) {
        // Implementation would control whether audio is processed
        // For now, this is a placeholder for power optimization
    }
    
    /**
     * Enable or disable background sync
     */
    fun setBackgroundSyncEnabled(enabled: Boolean) {
        // Implementation would control background data sync
        // For now, this is a placeholder for power optimization
    }
    
    /**
     * Set audio processing interval
     */
    fun setProcessingInterval(intervalMs: Long) {
        // Implementation would adjust how frequently audio is processed
        // For now, this is a placeholder for power optimization
    }
    
    /**
     * Set VAD sensitivity level
     */
    fun setVADSensitivity(sensitivity: Float) {
        // Implementation would adjust voice activity detection sensitivity
        // For now, this is a placeholder for power optimization
    }
    
    /**
     * Set audio buffer size for memory optimization
     */
    fun setAudioBufferSize(bufferSize: Int) {
        // Implementation would adjust audio buffer size
        // For now, this is a placeholder for power optimization
    }
    
    /**
     * Get current power optimization settings
     */
    fun getPowerOptimizationSettings(): PowerOptimizationSettings {
        return PowerOptimizationSettings(
            audioProcessingEnabled = true,
            backgroundSyncEnabled = true,
            processingIntervalMs = 1000,
            vadSensitivity = 0.5f,
            audioBufferSize = 2048
        )
    }
    
    /**
     * Service binder for local binding
     */
    inner class VoiceAgentBinder : Binder() {
        fun getService(): VoiceAgentService = this@VoiceAgentService
    }
}

/**
 * Listening state enumeration
 */
sealed class ListeningState {
    object STOPPED : ListeningState()
    object LISTENING : ListeningState()
    object PAUSED : ListeningState()
    object SLEEPING : ListeningState()
    data class ERROR(val message: String) : ListeningState()
}

/**
 * Audio processing callback interface
 */
interface AudioProcessingCallback {
    suspend fun onAudioChunkProcessed(
        audioData: ShortArray, 
        chunkId: String, 
        timestamp: Long,
        speakerResult: SpeakerResult? = null
    )
}

/**
 * Service statistics data class
 */
data class ServiceStats(
    val totalChunksProcessed: Int,
    val speechChunksDetected: Int,
    val currentState: ListeningState,
    val isInSleepMode: Boolean,
    val lastActivityTime: Long,
    val batteryLevel: Int
)

/**
 * Power optimization settings
 */
data class PowerOptimizationSettings(
    val audioProcessingEnabled: Boolean = true,
    val backgroundSyncEnabled: Boolean = true,
    val processingIntervalMs: Long = 1000,
    val vadSensitivity: Float = 0.5f,
    val audioBufferSize: Int = 2048
)

/**
 * Placeholder MainActivity class reference
 */
class MainActivity