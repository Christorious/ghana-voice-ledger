package com.voiceledger.ghana.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.voiceledger.ghana.data.local.dao.AudioMetadataDao
import com.voiceledger.ghana.data.local.dao.DailySummaryDao
import com.voiceledger.ghana.data.local.dao.ProductVocabularyDao
import com.voiceledger.ghana.data.local.dao.SpeakerProfileDao
import com.voiceledger.ghana.data.local.dao.TransactionDao
import com.voiceledger.ghana.data.local.entity.AudioMetadata
import com.voiceledger.ghana.data.local.entity.DailySummary
import com.voiceledger.ghana.data.local.entity.ProductVocabulary
import com.voiceledger.ghana.data.local.entity.SpeakerProfile
import com.voiceledger.ghana.data.local.entity.Transaction

/**
 * Main Room database for Ghana Voice Ledger.
 * Includes all entities and provides access to DAOs.
 */
@Database(
    entities = [
        Transaction::class,
        DailySummary::class,
        SpeakerProfile::class,
        ProductVocabulary::class,
        AudioMetadata::class
    ],
    version = 1,
    exportSchema = true
)
abstract class VoiceLedgerDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun dailySummaryDao(): DailySummaryDao
    abstract fun speakerProfileDao(): SpeakerProfileDao
    abstract fun productVocabularyDao(): ProductVocabularyDao
    abstract fun audioMetadataDao(): AudioMetadataDao

    companion object {
        const val DATABASE_NAME = "voice_ledger_database"
    }
}
