package com.voiceledger.ghana.ml.audio

/**
 * Audio processing utilities for Ghana Voice Ledger.
 *
 * The previous contents of this file were truncated mid-identifier (it ended
 * at `const val SAMPLE_RATE_16K`), and what survived was invalid anyway — an
 * `object` cannot declare a `companion object`. This restores the sample-rate
 * constants it was in the middle of declaring, plus [preprocessAudioForML],
 * which TensorFlowLiteSpeakerIdentifier calls on every embedding extraction.
 */
object AudioUtils {

    // Audio format constants
    const val SAMPLE_RATE_8KHZ = 8000
    const val SAMPLE_RATE_16KHZ = 16000
    const val SAMPLE_RATE_32KHZ = 32000
    const val SAMPLE_RATE_48KHZ = 48000

    /** The rate the capture pipeline and the on-device models expect. */
    const val DEFAULT_SAMPLE_RATE = SAMPLE_RATE_16KHZ

    /** 16-bit mono PCM: two bytes per sample. */
    const val BYTES_PER_SAMPLE = 2

    /** Largest magnitude of a signed 16-bit sample, used to scale to [-1, 1]. */
    private const val PCM16_FULL_SCALE = 32768f

    /**
     * Turn raw 16-bit little-endian mono PCM into the fixed-length float frame a
     * TensorFlow Lite model expects.
     *
     * Samples are scaled to [-1, 1]. A clip shorter than [targetSamples] is padded
     * with silence; a longer one is truncated. The model's input tensor is a fixed
     * shape, so the frame has to be exactly that length regardless of what was captured.
     */
    fun preprocessAudioForML(audioData: ByteArray, targetSamples: Int): FloatArray {
        val out = FloatArray(targetSamples)
        val available = minOf(targetSamples, audioData.size / BYTES_PER_SAMPLE)
        for (i in 0 until available) {
            val lo = audioData[i * 2].toInt() and 0xFF
            val hi = audioData[i * 2 + 1].toInt()        // keep the sign
            out[i] = ((hi shl 8) or lo).toShort() / PCM16_FULL_SCALE
        }
        return out
    }
}
