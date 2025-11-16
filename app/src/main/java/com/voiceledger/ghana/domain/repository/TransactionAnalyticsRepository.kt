package com.voiceledger.ghana.domain.repository

import com.voiceledger.ghana.domain.model.AnalyticsRange
import com.voiceledger.ghana.domain.model.TransactionAnalyticsOverview
import kotlinx.coroutines.flow.Flow

/**
 * Repository surface for reactive transaction analytics used by the analytics dashboard.
 */
interface TransactionAnalyticsRepository {
    fun observeAnalytics(range: AnalyticsRange = AnalyticsRange.Last30Days): Flow<TransactionAnalyticsOverview>
}
