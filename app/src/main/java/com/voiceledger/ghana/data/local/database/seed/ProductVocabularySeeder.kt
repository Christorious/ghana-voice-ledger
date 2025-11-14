package com.voiceledger.ghana.data.local.database.seed

import androidx.room.withTransaction
import com.voiceledger.ghana.data.local.database.VoiceLedgerDatabase
import timber.log.Timber

/**
 * # ProductVocabularySeeder
 * 
 * **Clean Architecture - Data Layer**
 * 
 * Orchestrates the seeding of product vocabulary data into the Room database during initial
 * database creation. This class ensures that when users first install the app, they have a
 * pre-populated vocabulary of fish products and measurement units for immediate voice recognition.
 * 
 * ## Purpose in the Seed Data Pipeline:
 * 
 * ```
 * assets/seed_data/products.json
 *            ↓
 *     SeedDataLoader (loads & parses JSON)
 *            ↓
 *  ProductVocabularySeeder (this class - orchestrates insertion)
 *            ↓
 *     Room Database (stores in SQLite)
 * ```
 * 
 * ## Key Responsibilities:
 * 
 * 1. **Duplicate Prevention**: Checks if products already exist before inserting
 * 2. **Transaction Management**: Ensures all-or-nothing insertion using Room transactions
 * 3. **Error Handling**: Propagates errors through the Result<T> pattern
 * 4. **Logging**: Provides visibility into the seeding process for debugging
 * 
 * ## Why Separate Loader from Seeder?
 * 
 * Following Single Responsibility Principle:
 * - **SeedDataLoader**: Knows how to read and parse JSON
 * - **ProductVocabularySeeder**: Knows how to insert into database with proper checks
 * 
 * This separation makes each class easier to test and maintain independently.
 * 
 * @property seedDataLoader Handles loading seed data from JSON assets
 */
internal class ProductVocabularySeeder(
    private val seedDataLoader: SeedDataLoader
) {

    /**
     * Seeds the database with initial product vocabulary.
     * 
     * ## The suspend Keyword:
     * 
     * `suspend` marks this function as a suspending function, which means it can be paused and
     * resumed without blocking a thread. This is part of Kotlin Coroutines - essential for
     * performing long-running operations like database I/O without freezing the app.
     * 
     * Suspending functions can only be called from:
     * 1. Other suspending functions
     * 2. Inside a coroutine (launched with launch, async, etc.)
     * 
     * ## Room Transactions with withTransaction:
     * 
     * `database.withTransaction { }` ensures that all database operations inside the block
     * happen atomically - either all succeed or all are rolled back. This is critical for
     * maintaining data integrity:
     * 
     * - If insertion fails halfway through, the database won't be left in an inconsistent state
     * - All 13 products are inserted together or none at all
     * - Other database operations are blocked until the transaction completes
     * 
     * ## Result<Int> Return Type:
     * 
     * Returns the number of products inserted wrapped in Result:
     * - `Result.success(13)`: Successfully inserted 13 products
     * - `Result.success(0)`: Skipped insertion (products already exist or empty seed data)
     * - `Result.failure(exception)`: An error occurred during loading or insertion
     * 
     * ## Duplicate Prevention via getActiveProductCount():
     * 
     * Before inserting, we check if products already exist. This prevents duplicate seed data
     * if the database is:
     * - Upgraded (onCreate won't run again)
     * - Restored from backup
     * - Already seeded in a previous attempt
     * 
     * ## The mapCatching Operation:
     * 
     * `mapCatching` is a Result extension that transforms success values while preserving
     * failures. Here's what happens:
     * 
     * 1. If `loadProductSeeds()` succeeds → execute the lambda with the seeds list
     * 2. If `loadProductSeeds()` fails → skip the lambda and propagate the failure
     * 3. If the lambda throws an exception → wrap it in Result.failure
     * 
     * This enables elegant error handling without nested try-catch blocks.
     * 
     * @param database The Room database instance to seed
     * @return Result containing the number of products inserted (0 if skipped, >0 if inserted)
     */
    suspend fun seed(database: VoiceLedgerDatabase): Result<Int> {
        // Load seed data from JSON, then transform the result
        return seedDataLoader.loadProductSeeds().mapCatching { seeds ->
            // Check if seed data was loaded successfully but is empty
            if (seeds.isEmpty()) {
                Timber.w("Product seed data is empty; skipping insertion")
                return@mapCatching 0
            }
            
            // Use a database transaction to ensure atomic insertion
            database.withTransaction {
                // Get the DAO for product vocabulary operations
                val dao = database.productVocabularyDao()
                
                // Check if products already exist (duplicate prevention)
                val existingCount = dao.getActiveProductCount()
                
                if (existingCount == 0) {
                    // Database is empty, safe to insert seed data
                    dao.insertProducts(seeds)
                    Timber.i("Inserted ${seeds.size} product vocabulary seed entries")
                    seeds.size
                } else {
                    // Products already exist, skip insertion to prevent duplicates
                    Timber.i("Product vocabulary already populated with $existingCount entries; skipping seed insertion")
                    0
                }
            }
        }
    }
}
