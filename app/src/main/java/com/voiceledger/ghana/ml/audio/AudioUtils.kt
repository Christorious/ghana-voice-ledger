package com.voiceledger.ghana.ml.audio

/**
 * Audio processing utilities for Ghana Voice Ledger.
 *
 * NOTE: the previous contents of this file were truncated mid-identifier
 * (it ended at `const val SAMPLE_RATE_16K`), and what survived was invalid
 * anyway — an `object` cannot declare a `companion object`. No call site in
 * the app references any member of AudioUtils: it is used only as a type
 * (`TensorFlowLiteSpeakerIdentifier(context, repo, AudioUtils)`) and mocked
 * in tests. This restores a valid singleton with the sample-rate constants
 * it was in the middle of declaring. Add helpers here as the real audio
 * pipeline needs them.
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
}
