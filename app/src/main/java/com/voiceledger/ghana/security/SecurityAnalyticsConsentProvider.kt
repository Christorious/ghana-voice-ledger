package com.voiceledger.ghana.security

import com.voiceledger.ghana.domain.service.AnalyticsConsentProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityAnalyticsConsentProvider @Inject constructor(
    private val securityManager: SecurityManager
) : AnalyticsConsentProvider {
    override fun isAnalyticsEnabled(): Boolean {
        return securityManager.isDataProcessingAllowed(DataProcessingPurpose.ANALYTICS)
    }
}
