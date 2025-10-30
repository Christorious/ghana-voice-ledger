package com.voiceledger.ghana.service

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioCaptureController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val AUDIO_SOURCE = MediaRecorder.AudioSource.MIC
        private const val BUFFER_SIZE_MULTIPLIER = 4
        private const val SAMPLES_PER_CHUNK = SAMPLE_RATE
    }

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val isRecording = AtomicBoolean(false)
    
    private val _audioChunks = MutableSharedFlow<AudioChunk>(
        replay = 0,
        extraBufferCapacity = 10,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val audioChunks: SharedFlow<AudioChunk> = _audioChunks.asSharedFlow()

    fun checkAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun initialize() {
        if (audioRecord != null) return
        
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

    fun startRecording(scope: CoroutineScope, delayMs: Long = 10) {
        if (isRecording.get()) return
        
        recordingJob = scope.launch(Dispatchers.IO) {
            audioRecord?.startRecording()
            isRecording.set(true)
            
            val buffer = ShortArray(SAMPLES_PER_CHUNK)
            
            while (isRecording.get() && isActive) {
                try {
                    val samplesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    
                    if (samplesRead > 0) {
                        _audioChunks.emit(
                            AudioChunk(
                                data = buffer.copyOf(samplesRead),
                                samplesRead = samplesRead,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                    
                    delay(delayMs)
                } catch (e: CancellationException) {
                    break
                } catch (e: Exception) {
                    _audioChunks.emit(
                        AudioChunk(
                            data = ShortArray(0),
                            samplesRead = 0,
                            timestamp = System.currentTimeMillis(),
                            error = e
                        )
                    )
                    break
                }
            }
        }
    }

    fun stopRecording() {
        isRecording.set(false)
        recordingJob?.cancel()
        audioRecord?.stop()
    }

    fun release() {
        stopRecording()
        audioRecord?.release()
        audioRecord = null
    }

    fun isActive(): Boolean = isRecording.get()
}

data class AudioChunk(
    val data: ShortArray,
    val samplesRead: Int,
    val timestamp: Long,
    val error: Exception? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AudioChunk

        if (!data.contentEquals(other.data)) return false
        if (samplesRead != other.samplesRead) return false
        if (timestamp != other.timestamp) return false
        if (error != other.error) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + samplesRead
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + (error?.hashCode() ?: 0)
        return result
    }
}
