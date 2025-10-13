package com.voiceledger.ghana.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.voiceledger.ghana.presentation.dashboard.ServiceState
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager class for VoiceAgentService interactions
 * Provides a clean interface for starting, stopping, and monitoring the service
 */
@Singleton
class VoiceAgentServiceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private var voiceAgentService: VoiceAgentService? = null
    private var isServiceBound = false
    
    private val _serviceConnectionState = MutableLiveData<ServiceConnectionState>()
    val serviceConnectionState: LiveData<ServiceConnectionState> = _serviceConnectionState
    
    private val _serviceState = MutableStateFlow(ServiceState())
    val serviceState: StateFlow<ServiceState> = _serviceState.asStateFlow()
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as VoiceAgentService.VoiceAgentBinder
            voiceAgentService = binder.getService()
            isServiceBound = true
            _serviceConnectionState.value = ServiceConnectionState.CONNECTED
            updateServiceState()
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            voiceAgentService = null
            isServiceBound = false
            _serviceConnectionState.value = ServiceConnectionState.DISCONNECTED
            _serviceState.value = ServiceState(
                isListening = false,
                status = "Disconnected",
                batteryLevel = 100
            )
        }
    }
    
    /**
     * Start and bind to the VoiceAgentService
     */
    fun startAndBindService() {
        val intent = Intent(context, VoiceAgentService::class.java)
        context.startService(intent)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        _serviceConnectionState.value = ServiceConnectionState.CONNECTING
    }
    
    /**
     * Unbind from the service
     */
    fun unbindService() {
        if (isServiceBound) {
            context.unbindService(serviceConnection)
            isServiceBound = false
            voiceAgentService = null
            _serviceConnectionState.value = ServiceConnectionState.DISCONNECTED
        }
    }
    
    /**
     * Start listening for audio
     */
    fun startListening() {
        val intent = Intent(context, VoiceAgentService::class.java).apply {
            action = VoiceAgentService.ACTION_START_LISTENING
        }
        context.startService(intent)
        updateServiceState()
    }
    
    /**
     * Pause listening
     */
    fun pauseListening() {
        val intent = Intent(context, VoiceAgentService::class.java).apply {
            action = VoiceAgentService.ACTION_PAUSE_LISTENING
        }
        context.startService(intent)
        updateServiceState()
    }
    
    /**
     * Stop listening and service
     */
    fun stopListening() {
        val intent = Intent(context, VoiceAgentService::class.java).apply {
            action = VoiceAgentService.ACTION_STOP_LISTENING
        }
        context.startService(intent)
    }
    
    /**
     * Get current listening state
     */
    fun getListeningState(): StateFlow<ListeningState>? {
        return voiceAgentService?.listeningState
    }
    
    /**
     * Get service statistics
     */
    fun getServiceStats(): ServiceStats? {
        return voiceAgentService?.getServiceStats()
    }
    
    /**
     * Set audio processing callback
     */
    fun setAudioProcessingCallback(callback: AudioProcessingCallback?) {
        voiceAgentService?.audioProcessingCallback = callback
    }
    
    /**
     * Check if service is currently bound
     */
    fun isServiceBound(): Boolean = isServiceBound
    
    /**
     * Check if service is currently listening
     */
    fun isListening(): Boolean {
        return when (voiceAgentService?.listeningState?.value) {
            is ListeningState.LISTENING, is ListeningState.SLEEPING -> true
            else -> false
        }
    }
    
    /**
     * Check if service is paused
     */
    fun isPaused(): Boolean {
        return voiceAgentService?.listeningState?.value is ListeningState.PAUSED
    }
    
    /**
     * Check if service has error
     */
    fun hasError(): String? {
        return when (val state = voiceAgentService?.listeningState?.value) {
            is ListeningState.ERROR -> state.message
            else -> null
        }
    }
    
    /**
     * Update the service state flow
     */
    private fun updateServiceState() {
        val service = voiceAgentService
        if (service != null) {
            val listeningState = service.listeningState.value
            val stats = service.getServiceStats()
            
            _serviceState.value = ServiceState(
                isListening = when (listeningState) {
                    is ListeningState.LISTENING, is ListeningState.SLEEPING -> true
                    else -> false
                },
                status = when (listeningState) {
                    is ListeningState.LISTENING -> "Listening"
                    is ListeningState.SLEEPING -> "Sleeping"
                    is ListeningState.PAUSED -> "Paused"
                    is ListeningState.STOPPED -> "Stopped"
                    is ListeningState.ERROR -> "Error: ${listeningState.message}"
                    else -> "Unknown"
                },
                batteryLevel = stats?.batteryLevel ?: 100
            )
        } else {
            _serviceState.value = ServiceState(
                isListening = false,
                status = "Not Connected",
                batteryLevel = 100
            )
        }
    }
    
    // Power Optimization Methods
    
    /**
     * Enable or disable audio processing
     */
    suspend fun setAudioProcessingEnabled(enabled: Boolean) {
        voiceAgentService?.setAudioProcessingEnabled(enabled)
    }
    
    /**
     * Enable or disable background sync
     */
    suspend fun setBackgroundSyncEnabled(enabled: Boolean) {
        voiceAgentService?.setBackgroundSyncEnabled(enabled)
    }
    
    /**
     * Set audio processing interval
     */
    suspend fun setProcessingInterval(intervalMs: Long) {
        voiceAgentService?.setProcessingInterval(intervalMs)
    }
    
    /**
     * Set VAD sensitivity level
     */
    suspend fun setVADSensitivity(sensitivity: Float) {
        voiceAgentService?.setVADSensitivity(sensitivity)
    }
    
    /**
     * Set audio buffer size for memory optimization
     */
    suspend fun setAudioBufferSize(bufferSize: Int) {
        voiceAgentService?.setAudioBufferSize(bufferSize)
    }
    
    /**
     * Pause service for power saving
     */
    suspend fun pauseService() {
        pauseListening()
    }
    
    /**
     * Resume service from power saving mode
     */
    suspend fun resumeService() {
        startListening()
    }
    
    /**
     * Get current power optimization settings
     */
    fun getPowerOptimizationSettings(): PowerOptimizationSettings? {
        return voiceAgentService?.getPowerOptimizationSettings()
    }
}

/**
 * Service connection state enumeration
 */
enum class ServiceConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

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