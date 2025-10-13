package com.voiceledger.ghana.di

import android.content.Context
import com.voiceledger.ghana.data.local.database.VoiceLedgerDatabase
import com.voiceledger.ghana.performance.DatabaseOptimizer
import com.voiceledger.ghana.performance.MemoryManager
import com.voiceledger.ghana.performance.PerformanceMonitor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for performance optimization dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object PerformanceModule {

    @Provides
    @Singleton
    fun provideMemoryManager(
        @ApplicationContext context: Context
    ): MemoryManager {
        return MemoryManager(context)
    }

    @Provides
    @Singleton
    fun provideDatabaseOptimizer(
        database: VoiceLedgerDatabase
    ): DatabaseOptimizer {
        return DatabaseOptimizer(database)
    }

    @Provides
    @Singleton
    fun providePerformanceMonitor(): PerformanceMonitor {
        return PerformanceMonitor()
    }
}