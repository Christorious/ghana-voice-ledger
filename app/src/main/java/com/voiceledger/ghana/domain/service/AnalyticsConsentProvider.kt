package com.voiceledger.ghana.domain.service

/**
 * Abstraction that determines whether analytics processing is permitted by user privacy settings.
 */
interface AnalyticsConsentProvider {
    fun isAnalyticsEnabled(): Boolean
}
