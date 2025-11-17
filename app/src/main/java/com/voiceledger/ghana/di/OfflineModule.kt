package com.voiceledger.ghana.di

import android.content.Context
import com.voiceledger.ghana.data.local.database.VoiceLedgerDatabase
import com.voiceledger.ghana.offline.ConflictResolver
import com.voiceledger.ghana.offline.OfflineFirstRepository
import com.voiceledger.ghana.offline.OfflineQueueManager
import com.voiceledger.ghana.offline.OfflineTransactionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for offline-first architecture dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object OfflineModule {

    @Provides
    @Singleton
    fun provideOfflineQueueManager(
        @ApplicationContext context: Context,
        database: VoiceLedgerDatabase
    ): OfflineQueueManager {
        return OfflineQueueManager(
            context = context,
            operationDao = database.offlineOperationDao()
        )
    }

    @Provides
    @Singleton
    fun provideConflictResolver(): ConflictResolver {
        return ConflictResolver()
    }

    @Provides
    @Singleton
    fun provideOfflineTransactionRepository(
        @ApplicationContext context: Context,
        offlineQueueManager: OfflineQueueManager,
        database: VoiceLedgerDatabase
    ): OfflineTransactionRepository {
        return OfflineTransactionRepository(
            context = context,
            offlineQueueManager = offlineQueueManager,
            localTransactionDao = database.transactionDao(),
            remoteTransactionApi = null // Would be provided by network module
        )
    }
}