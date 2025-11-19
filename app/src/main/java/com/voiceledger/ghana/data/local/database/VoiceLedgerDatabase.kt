package com.voiceledger.ghana.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.voiceledger.ghana.BuildConfig
import com.voiceledger.ghana.data.local.dao.*
import com.voiceledger.ghana.data.local.entity.*
import com.voiceledger.ghana.data.local.database.seed.ProductVocabularySeeder
import com.voiceledger.ghana.data.local.database.seed.SeedDataLoader
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * # VoiceLedgerDatabase
 * 
 * **Clean Architecture - Data Layer**
 * 
 * The central Room database for the Ghana Voice Ledger application. This class serves as the
 * main access point to the SQLite database, managing all persistent data for the app.
 * 
 * ## What is Room?
 * 
 * Room is Android's recommended database library. It sits on top of SQLite and provides:
 * - Compile-time SQL verification (catch errors before runtime)
 * - Easy object mapping (convert database rows to Kotlin objects automatically)
 * - Lifecycle awareness (integrates with Android components)
 * - Type-safe queries (no more string concatenation for SQL)
 * 
 * ## The @Database Annotation:
 * 
 * This annotation marks the class as a Room database and configures it:
 * 
 * - **entities**: Lists all the database tables (Kotlin classes marked with @Entity)
 * - **version**: Database schema version number. Increment when you change the structure
 * - **exportSchema**: When true, Room exports the database schema to a JSON file for
 *   version control and migration planning
 * 
 * ## Abstract Class Pattern:
 * 
 * Room requires database classes to be abstract because Room generates the implementation
 * at compile time. We define what we need (DAOs), and Room creates the concrete class.
 * 
 * ## Database Entities (Tables):
 * 
 * - **Transaction**: Individual sales transactions from voice commands
 * - **DailySummary**: Aggregated daily business metrics and insights
 * - **SpeakerProfile**: Voice profiles for speaker identification
 * - **ProductVocabulary**: Known products and their language variants
 * - **AudioMetadata**: Metadata about recorded audio clips
 * - **OfflineOperationEntity/OfflineOperation**: Queue for operations to sync when online
 * 
 * ## Clean Architecture Placement:
 * 
 * This database sits in the Data Layer, hidden behind repository interfaces (Domain Layer).
 * The Presentation Layer (UI) never directly accesses this database - it goes through
 * repositories, maintaining proper separation of concerns.
 */
@Database(
    entities = [
        Transaction::class,
        DailySummary::class,
        SpeakerProfile::class,
        ProductVocabulary::class,
        AudioMetadata::class,
        OfflineOperationEntity::class,
        OfflineOperation::class
    ],
    version = 2,
    exportSchema = true
)
abstract class VoiceLedgerDatabase : RoomDatabase() {

    // DAOs (Data Access Objects) provide methods to interact with database tables
    // Room automatically implements these at compile time
    
    /**
     * Provides access to transaction-related database operations.
     * 
     * @return DAO for the Transaction entity (sales records)
     */
    abstract fun transactionDao(): TransactionDao
    
    /**
     * Provides access to daily summary database operations.
     * 
     * @return DAO for the DailySummary entity (aggregated daily metrics)
     */
    abstract fun dailySummaryDao(): DailySummaryDao
    
    /**
     * Provides access to speaker profile database operations.
     * 
     * @return DAO for the SpeakerProfile entity (voice identification data)
     */
    abstract fun speakerProfileDao(): SpeakerProfileDao
    
    /**
     * Provides access to product vocabulary database operations.
     * 
     * @return DAO for the ProductVocabulary entity (recognized products/units)
     */
    abstract fun productVocabularyDao(): ProductVocabularyDao
    
    /**
     * Provides access to audio metadata database operations.
     * 
     * @return DAO for the AudioMetadata entity (recorded audio information)
     */
    abstract fun audioMetadataDao(): AudioMetadataDao
    
    /**
     * Provides access to offline operation queue database operations.
     * 
     * @return DAO for the OfflineOperation entity (pending sync operations)
     */
    abstract fun offlineOperationDao(): OfflineOperationDao

    companion object {
        /**
         * The name of the database file in SQLite.
         * 
         * This constant is used throughout the app to ensure consistent database naming.
         */
        const val DATABASE_NAME = "voice_ledger_database"
        
        /**
         * The singleton database instance.
         * 
         * ## @Volatile Annotation:
         * 
         * This ensures that writes to INSTANCE are immediately visible to all threads.
         * Without @Volatile, one thread might cache the old value and not see updates
         * from other threads. This is crucial for thread-safe singleton patterns.
         * 
         * ## Why Nullable (VoiceLedgerDatabase?):
         * 
         * The instance is null until the database is first created. Kotlin's null safety
         * forces us to handle this case explicitly, preventing null pointer crashes.
         */
        @Volatile
        private var INSTANCE: VoiceLedgerDatabase? = null
        
        /**
         * Migration from database version 1 to version 2.
         * 
         * ## What are Database Migrations?
         * 
         * When you change your database schema (add tables, modify columns, etc.), existing
         * users have the old schema on their devices. Migrations tell Room how to transform
         * their old database into the new structure without losing data.
         * 
         * ## The Migration Object Pattern:
         * 
         * `object : Migration(1, 2)` creates an anonymous object (Kotlin's way of implementing
         * interfaces/abstract classes inline) that migrates from version 1 to version 2.
         * 
         * ## What This Migration Does:
         * 
         * Adds the `offline_operations` table for storing operations that need to be synced
         * when the device comes back online. This implements a durable queue pattern for
         * offline-first functionality.
         * 
         * ## Database Indices:
         * 
         * Indices are created on frequently queried columns (operationType, timestamp, synced,
         * retryCount) to speed up queries. Without indices, SQLite would have to scan the
         * entire table (slow). With indices, lookups are nearly instant.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create the new offline operations table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS offline_operations (
                        id TEXT NOT NULL PRIMARY KEY,
                        operationType TEXT NOT NULL,
                        entityType TEXT NOT NULL,
                        entityId TEXT NOT NULL,
                        data TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        synced INTEGER NOT NULL DEFAULT 0,
                        retryCount INTEGER NOT NULL DEFAULT 0,
                        maxRetries INTEGER NOT NULL DEFAULT 3,
                        lastError TEXT,
                        priority INTEGER NOT NULL DEFAULT 3,
                        processing INTEGER NOT NULL DEFAULT 0
                    )
                """)
                
                // Create indices for common query patterns (improves performance)
                database.execSQL("CREATE INDEX IF NOT EXISTS index_offline_operations_operationType ON offline_operations(operationType)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_offline_operations_timestamp ON offline_operations(timestamp)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_offline_operations_synced ON offline_operations(synced)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_offline_operations_retryCount ON offline_operations(retryCount)")
            }
        }
        
        /**
         * Factory method to create a database callback for seed data loading.
         * 
         * ## Why a Factory Method?
         * 
         * Instead of exposing the DatabaseCallback class directly, we provide a factory method.
         * This gives us:
         * 1. **Encapsulation**: The internal DatabaseCallback class stays private
         * 2. **Flexibility**: We can change the implementation without breaking callers
         * 3. **Testability**: Easy to inject a custom dispatcher for tests
         * 
         * ## The Dispatcher Parameter:
         * 
         * `dispatcher: CoroutineDispatcher = Dispatchers.IO` specifies which thread pool
         * to use for database operations:
         * - **Dispatchers.IO**: Optimized for I/O operations (database, files, network)
         * - **Dispatchers.Main**: UI thread (for updating the UI)
         * - **Dispatchers.Default**: CPU-intensive work (calculations, parsing)
         * 
         * The default is Dispatchers.IO since we're doing database I/O. Tests can inject
         * Dispatchers.Unconfined for immediate execution.
         * 
         * ## BuildConfig.DEBUG Usage:
         * 
         * Inside the callback, we check BuildConfig.DEBUG to decide whether to throw exceptions
         * or just log errors. This provides:
         * - **Debug builds**: Fail fast to catch issues during development
         * - **Release builds**: Graceful degradation for better user experience
         * 
         * @param context Android context for accessing assets
         * @param dispatcher Coroutine dispatcher for background work (injectable for testing)
         * @return Callback that will seed the database when onCreate is called
         */
        internal fun createSeedDataCallback(
            context: Context,
            dispatcher: CoroutineDispatcher = Dispatchers.IO
        ): RoomDatabase.Callback {
            return DatabaseCallback(context.applicationContext, dispatcher)
        }
    }
    
    /**
     * # DatabaseCallback
     * 
     * **Inner Class for Database Lifecycle Events**
     * 
     * This callback handles database lifecycle events, specifically onCreate, which fires
     * the first time the database file is created on a device. We use it to pre-populate
     * the database with initial product vocabulary.
     * 
     * ## Why an Inner Class?
     * 
     * Being a private inner class keeps the implementation details hidden while allowing
     * access to the outer class's companion object (specifically, the INSTANCE variable).
     * 
     * ## Kotlin Constructor Properties:
     * 
     * The constructor parameters without `val` or `var` are not stored as properties.
     * We manually create properties (applicationContext, databaseScope, etc.) because
     * we need to configure them or derive them from constructor parameters.
     * 
     * @property applicationContext Application-level context (never leaks, safe to hold long-term)
     * @property databaseScope Coroutine scope for background database operations
     * @property seedDataLoader Loads seed data from JSON assets
     * @property seeder Orchestrates seed data insertion into the database
     */
    private class DatabaseCallback(
        context: Context,
        dispatcher: CoroutineDispatcher = Dispatchers.IO
    ) : RoomDatabase.Callback() {
        
        // Store application context to avoid memory leaks (never hold Activity context!)
        private val applicationContext = context.applicationContext
        
        /**
         * Coroutine scope for database initialization work.
         * 
         * ## SupervisorJob():
         * 
         * Creates a job that doesn't cancel sibling coroutines if one fails. Without this,
         * if one seed operation failed, it could cancel other unrelated operations.
         * 
         * ## Operator Overloading (+):
         * 
         * Kotlin lets you overload operators. Here, `+` combines CoroutineContext elements:
         * SupervisorJob() + dispatcher creates a context with both the job and dispatcher.
         */
        private val databaseScope = CoroutineScope(SupervisorJob() + dispatcher)
        
        // Initialize seed data components
        private val seedDataLoader = SeedDataLoader(applicationContext)
        private val seeder = ProductVocabularySeeder(seedDataLoader)
        
        /**
         * Called when the database is created for the first time.
         * 
         * ## Callback Lifecycle:
         * 
         * Room calls this automatically the first time the app needs the database:
         * 1. App starts and requests database
         * 2. Room checks if database file exists
         * 3. If not, Room creates the file and calls onCreate
         * 4. We populate seed data
         * 
         * Important: This only runs once per device, not every app launch!
         * 
         * ## launch { }:
         * 
         * Starts a new coroutine in databaseScope. The coroutine runs asynchronously,
         * allowing onCreate to return immediately (non-blocking). This prevents ANR
         * (Application Not Responding) errors during database initialization.
         * 
         * @param db The native SQLite database instance (low-level access)
         */
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            
            // Launch seed data population in the background
            databaseScope.launch {
                populateInitialData()
            }
        }
        
        /**
         * Populates the database with initial product vocabulary.
         * 
         * ## suspend Function:
         * 
         * This is a suspending function that can pause execution without blocking threads.
         * It's called from within a coroutine launched in onCreate.
         * 
         * ## The Elvis Operator (?: ):
         * 
         * `INSTANCE ?: run { ... }` means:
         * - If INSTANCE is not null, use it
         * - Otherwise, execute the block after ?:
         * 
         * The `run` block logs a warning and returns early, preventing null pointer crashes.
         * 
         * ## Result API Pattern:
         * 
         * We use `.onSuccess { }` and `.onFailure { }` to handle both cases without
         * try-catch blocks. This is more functional and composable.
         * 
         * ## BuildConfig.DEBUG Check:
         * 
         * In debug builds (when developing), we throw exceptions to catch problems immediately.
         * In release builds (production), we just log errors so the app doesn't crash for users.
         * This is a common pattern: fail fast in development, degrade gracefully in production.
         */
        private suspend fun populateInitialData() {
            // Get the database instance (should be set by now, but check to be safe)
            val database = INSTANCE ?: run {
                Timber.w("Database instance not available for seed data loading")
                return
            }
            
            try {
                // Attempt to seed the database
                val result = seeder.seed(database)
                
                // Handle success case
                result.onSuccess { insertedCount ->
                    if (insertedCount > 0) {
                        Timber.i("Seeded $insertedCount product vocabulary entries from assets")
                    } else {
                        Timber.i("Product vocabulary already populated; no seed entries inserted")
                    }
                }
                // Handle failure case
                .onFailure { error ->
                    Timber.e(error, "Failed to seed product vocabulary from assets")
                    // In debug builds, crash to alert developers
                    if (BuildConfig.DEBUG) {
                        throw IllegalStateException("Failed to seed product vocabulary", error)
                    }
                    // In release builds, the app continues without seed data
                }
            } catch (error: Exception) {
                // Catch any unexpected errors (should be rare with Result pattern)
                Timber.e(error, "Unexpected error during product vocabulary seeding")
                if (BuildConfig.DEBUG) {
                    throw IllegalStateException("Unexpected error during product vocabulary seeding", error)
                }
            }
        }
    }
}
