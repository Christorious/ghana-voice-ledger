package com.voiceledger.ghana.ml.audio

import kotlin.math.*

/**
 * Audio processing utilities for Ghana Voice Ledger
 * Provides common audio processing functions for VAD and speech recognition
 */
object AudioUtils {
    
    companion object {
        // Audio format constants
        const val SAMPLE_RATE_8KHZ = 8000
        const val SAMPLE_RATE_16K