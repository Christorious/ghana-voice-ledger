package com.voiceledger.ghana.di

import android.content.Context
import com.voiceledger.ghana.analytics.AnalyticsService
import com.voiceledger.ghana.analytics.CrashlyticsService
import com.voiceledger.ghana.analytics.PerformanceMonitoringService
import com.voiceledger.ghana.analytics.UsageDashboardService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for analytics and monitoring components
 */
@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {
    
    @Provides
    @Singleton
    fun provideAnalyticsService(
        @ApplicationContext context: Context
    ): AnalyticsService {
        return AnalyticsService(context)
    }
    
    @Provides
    @Singleton
    fun provideCrashlyticsService(
        @ApplicationContext context: Context
    ): CrashlyticsService {
        return CrashlyticsService(context)
    }
    
    @Provides
    @Singleton
    fun providePerformanceMonitoringService(
        @ApplicationContext context: Context,
        analyticsService: AnalyticsService,
        crashlyticsService: CrashlyticsService
    ): PerformanceMonitoringService {
        return PerformanceMonitoringService(context, analyticsService, crashlyticsService)
    }
    
    @Provides
    @Singleton
    fun provideUsageDashboardService(
        @ApplicationContext context: Context,
        analyticsService: AnalyticsService,
        crashlyticsService: CrashlyticsService
    ): UsageDashboardService {
        return UsageDashboardService(context, analyticsService, crashlyticsService)
    }
}