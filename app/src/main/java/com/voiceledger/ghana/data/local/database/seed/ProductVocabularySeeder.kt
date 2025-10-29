package com.voiceledger.ghana.data.local.database.seed

import androidx.room.withTransaction
import com.voiceledger.ghana.data.local.database.VoiceLedgerDatabase
import timber.log.Timber

internal class ProductVocabularySeeder(
    private val seedDataLoader: SeedDataLoader
) {

    suspend fun seed(database: VoiceLedgerDatabase): Result<Int> {
        return seedDataLoader.loadProductSeeds().mapCatching { seeds ->
            if (seeds.isEmpty()) {
                Timber.w("Product seed data is empty; skipping insertion")
                0
            } else {
                database.withTransaction {
                    val dao = database.productVocabularyDao()
                    val existingCount = dao.getActiveProductCount()
                    if (existingCount == 0) {
                        dao.insertProducts(seeds)
                        Timber.i("Inserted ${seeds.size} product vocabulary seed entries")
                        seeds.size
                    } else {
                        Timber.i("Product vocabulary already populated with $existingCount entries; skipping seed insertion")
                        0
                    }
                }
            }
        }
    }
}
