package com.voiceledger.ghana.service

import com.voiceledger.ghana.data.local.entity.AudioMetadata
import com.voiceledger.ghana.domain.repository.AudioMetadataRepository
import com.voiceledger.ghana.ml.speaker.SpeakerIdentifier
import com.voiceledger.ghana.ml.speaker.SpeakerResult
import com.voiceledger.ghana.ml.speaker.SpeakerType
import com.voiceledger.ghana.ml.speech.SpeechRecognitionManager
import com.voiceledger.ghana.ml.speech.TranscriptionResult
import com.voiceledger.ghana.ml.transaction.TransactionProcessor
import com.voiceledger.ghana.ml.vad.VADManager
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeechProcessingPipeline @Inject constructor(
    private val vadManager: VADManager,
    private val speakerIdentifier: SpeakerIdentifier,
    private val speechRecognitionManager: SpeechRecognitionManager,
    private val transactionProcessor: TransactionProcessor,
    private val audioMetadataRepository: AudioMetadataRepository,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    companion object {
        private const val CHUNK_DURATION_MS = 1000L
        private const val VAD_CONFIDENCE_THRESHOLD = 0.3f
    }

    private val supervisorJob = SupervisorJob()
    private val processingScope = CoroutineScope(supervisorJob + defaultDispatcher)
    private val _results = MutableSharedFlow<ProcessingResult>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val results: SharedFlow<ProcessingResult> = _results.asSharedFlow()

    suspend fun initialize() = coroutineScope {
        val vadInit = async { vadManager.initialize(com.voiceledger.ghana.ml.vad.VADType.CUSTOM) }
        val speakerInit = async { speakerIdentifier.initialize() }
        val speechInit = async { speechRecognitionManager.optimizeForMarketEnvironment() }
        vadInit.await()
        speakerInit.await()
        speechInit.await()
    }

    fun submitChunk(
        audioChunk: AudioChunk,
        batteryLevel: Int,
        powerSavingMode: Boolean,
        callback: AudioProcessingCallback?
    ) {
        processingScope.launch {
            val result = processChunkInternal(audioChunk, batteryLevel, powerSavingMode, callback)
            _results.emit(result)
        }
    }

    fun startProcessing() {
        vadManager.startProcessing()
    }

    fun stopProcessing() {
        vadManager.stopProcessing()
        processingScope.coroutineContext[Job]?.cancelChildren()
    }

    fun cleanup() {
        vadManager.destroy()
        speakerIdentifier.cleanup()
        speechRecognitionManager.cleanup()
        transactionProcessor.cleanup()
        processingScope.cancel()
    }

    private suspend fun processChunkInternal(
        audioChunk: AudioChunk,
        batteryLevel: Int,
        powerSavingMode: Boolean,
        callback: AudioProcessingCallback?
    ): ProcessingResult {
        val chunkId = "chunk_${audioChunk.timestamp}_${UUID.randomUUID()}"
        val processingStartTime = System.currentTimeMillis()

        return try {
            val audioBytes = convertShortsToBytes(audioChunk.data, audioChunk.samplesRead)

            val vadResult = withContext(defaultDispatcher) {
                vadManager.processAudioSample(audioBytes)
            }

            val hasActivity = vadResult.isSpeech && vadResult.confidence > VAD_CONFIDENCE_THRESHOLD

            var speakerResult: SpeakerResult? = null
            var transcriptionResult: TranscriptionResult? = null

            if (hasActivity) {
                coroutineScope {
                    val speakerDeferred = async(defaultDispatcher) {
                        speakerIdentifier.identifySpeaker(audioBytes)
                    }
                    val transcriptionDeferred = async(defaultDispatcher) {
                        speechRecognitionManager.transcribe(audioBytes)
                    }
                    speakerResult = speakerDeferred.await()
                    transcriptionResult = transcriptionDeferred.await()
                }

                if (transcriptionResult?.isSuccess == true && !transcriptionResult?.transcript.isNullOrBlank()) {
                    val isSeller = speakerResult?.speakerType == SpeakerType.SELLER
                    val speakerId = speakerResult?.speakerId ?: "unknown"
                    withContext(defaultDispatcher) {
                        transactionProcessor.processUtterance(
                            transcript = transcriptionResult!!.transcript,
                            speakerId = speakerId,
                            isSeller = isSeller,
                            confidence = transcriptionResult!!.confidence,
                            timestamp = audioChunk.timestamp,
                            audioChunkId = chunkId
                        )
                    }
                }

                callback?.let {
                    withContext(defaultDispatcher) {
                        it.onAudioChunkProcessed(
                            audioChunk.data.copyOf(audioChunk.samplesRead),
                            chunkId,
                            audioChunk.timestamp,
                            speakerResult
                        )
                    }
                }
            }

            val metadata = AudioMetadata(
                chunkId = chunkId,
                timestamp = audioChunk.timestamp,
                vadScore = vadResult.confidence,
                speechDetected = hasActivity,
                speakerDetected = speakerResult != null && speakerResult?.speakerType != SpeakerType.UNKNOWN,
                speakerId = speakerResult?.speakerId,
                speakerConfidence = speakerResult?.confidence,
                audioQuality = vadResult.energyLevel,
                durationMs = CHUNK_DURATION_MS,
                processingTimeMs = System.currentTimeMillis() - processingStartTime,
                contributedToTransaction = false,
                transactionId = null,
                errorMessage = null,
                batteryLevel = batteryLevel,
                powerSavingMode = powerSavingMode
            )

            withContext(ioDispatcher) {
                audioMetadataRepository.insertMetadata(metadata)
            }

            ProcessingResult.Success(
                chunkId = chunkId,
                timestamp = audioChunk.timestamp,
                vadConfidence = vadResult.confidence,
                hasActivity = hasActivity,
                speakerResult = speakerResult,
                transcriptionResult = transcriptionResult
            )
        } catch (e: Exception) {
            val errorMetadata = AudioMetadata(
                chunkId = chunkId,
                timestamp = audioChunk.timestamp,
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
                batteryLevel = batteryLevel,
                powerSavingMode = powerSavingMode
            )

            withContext(ioDispatcher) {
                audioMetadataRepository.insertMetadata(errorMetadata)
            }

            ProcessingResult.Error(
                chunkId = chunkId,
                timestamp = audioChunk.timestamp,
                exception = e
            )
        }
    }

    private fun convertShortsToBytes(buffer: ShortArray, length: Int): ByteArray {
        val bytes = ByteArray(length * 2)
        for (i in 0 until length) {
            val sample = buffer[i]
            bytes[i * 2] = (sample.toInt() and 0xFF).toByte()
            bytes[i * 2 + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
        }
        return bytes
    }
}

sealed class ProcessingResult {
    data class Success(
        val chunkId: String,
        val timestamp: Long,
        val vadConfidence: Float,
        val hasActivity: Boolean,
        val speakerResult: SpeakerResult?,
        val transcriptionResult: TranscriptionResult?
    ) : ProcessingResult()

    data class Error(
        val chunkId: String,
        val timestamp: Long,
        val exception: Exception
    ) : ProcessingResult()
}
