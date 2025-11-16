package com.voiceledger.ghana.di

import android.content.Context
import androidx.room.Room
import com.voiceledger.ghana.data.local.dao.*
import com.voiceledger.ghana.data.local.database.DatabaseFactory
import com.voiceledger.ghana.data.local.database.DatabaseMigrations
import com.voiceledger.ghana.data.local.database.VoiceLedgerDatabase
import com.voiceledger.ghana.data.repository.*
import com.voiceledger.ghana.domain.repository.*
import com.voiceledger.ghana.domain.service.AnalyticsConsentProvider
import com.voiceledger.ghana.security.SecurityManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.room.SupportFactory
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * # DatabaseModule
 * 
 * **Dependency Injection - Hilt Module**
 * 
 * This Hilt module is the central configuration point for database-related dependency injection.
 * It tells Hilt how to create and provide database instances, DAOs, and repositories throughout
 * the app.
 * 
 * ## What is Dependency Injection (DI)?
 * 
 * Dependency Injection is a design pattern where objects receive their dependencies from external
 * sources rather than creating them. Instead of:
 * 
 * ```kotlin
 * class MyViewModel {
 *     val repository = TransactionRepositoryImpl(TransactionDao())  // Hard-coded dependency
 * }
 * ```
 * 
 * We use DI:
 * 
 * ```kotlin
 * class MyViewModel @Inject constructor(
 *     private val repository: TransactionRepository  // Injected dependency
 * )
 * ```
 * 
 * Benefits:
 * 1. **Testability**: Easy to inject mock dependencies for testing
 * 2. **Flexibility**: Change implementations without modifying consumers
 * 3. **Decoupling**: Classes don't know how their dependencies are created
 * 4. **Lifecycle Management**: Hilt handles creating and destroying objects
 * 
 * ## What is Hilt?
 * 
 * Hilt is Android's recommended dependency injection framework (built on Dagger). It automatically
 * generates the code needed to inject dependencies into Android components (Activities, Fragments,
 * ViewModels, Services, etc.).
 * 
 * ## The @Module Annotation:
 * 
 * Marks this as a Hilt module - a class that tells Hilt how to provide certain types.
 * Modules contain @Provides methods that create and configure dependencies.
 * 
 * ## @InstallIn(SingletonComponent::class):
 * 
 * Specifies the component lifecycle for dependencies provided by this module:
 * - **SingletonComponent**: Dependencies live for the entire app lifetime
 * - Alternative components: ActivityComponent, FragmentComponent, ViewModelComponent
 * 
 * This means our database and repositories are singletons - only one instance exists app-wide,
 * shared by all screens. This is correct for databases to avoid multiple connections.
 * 
 * ## object Declaration:
 * 
 * Kotlin's `object` creates a singleton. Since this module doesn't hold state and just provides
 * factories, a singleton is perfect. All @Provides methods are like static methods in Java.
 * 
 * ## How Hilt Uses This Module:
 * 
 * 1. App starts
 * 2. Hilt scans for @Module annotations
 * 3. Hilt builds a dependency graph
 * 4. When a class needs a dependency (e.g., TransactionRepository), Hilt:
 *    - Finds the @Provides method that returns TransactionRepository
 *    - Recursively resolves that method's dependencies (TransactionDao)
 *    - Calls the methods in order
 *    - Injects the final object
 * 
 * This all happens automatically with zero boilerplate in consuming classes!
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    private const val USE_ENCRYPTION = false
    private const val DATABASE_PASSPHRASE = "ghana_voice_ledger_secure_key_2024"
    
    /**
     * Provides the main Room database instance.
     * 
     * ## The @Provides Annotation:
     * 
     * Tells Hilt "when something needs a VoiceLedgerDatabase, call this method to create it".
     * Hilt will automatically inject the parameters (context) from other @Provides methods.
     * 
     * ## The @Singleton Annotation:
     * 
     * Ensures only one database instance exists for the entire app lifetime. Without this,
     * Hilt would create a new database every time one is requested, which would:
     * - Waste memory
     * - Cause concurrency issues
     * - Break transactions across different instances
     * 
     * ## The @ApplicationContext Qualifier:
     * 
     * Hilt knows how to provide several types of Context:
     * - @ApplicationContext: App-level context (safe to hold long-term)
     * - @ActivityContext: Activity context (only for activity-scoped dependencies)
     * 
     * We use @ApplicationContext because databases live beyond individual activities.
     * 
     * ## Encryption Toggle:
     * 
     * Currently disabled (USE_ENCRYPTION = false) for development simplicity, but the
     * infrastructure exists to enable SQLCipher encryption for production builds protecting
     * sensitive transaction data.
     * 
     * ## Return Type:
     * 
     * Returns VoiceLedgerDatabase (the abstract class). Hilt and consumers work with the
     * abstract type, not the concrete implementation, following the Dependency Inversion
     * Principle from SOLID.
     * 
     * @param context Application context for database file location
     * @return Singleton database instance
     */
    @Provides
    @Singleton
    fun provideVoiceLedgerDatabase(@ApplicationContext context: Context): VoiceLedgerDatabase {
        return if (USE_ENCRYPTION) {
            provideEncryptedVoiceLedgerDatabase(context, buildSecurityManager())
        } else {
            DatabaseFactory.createDatabase(
                context = context,
                encrypted = false
            )
        }
    }
    
    private fun buildSecurityManager(): SecurityManager {
        return SecurityManager.getInstance()
    }
    
    /**
     * Provides encrypted database instance for production use
     * This would be used when security is required
     */
    @Provides
    @Singleton
    @EncryptedDatabase
    fun provideEncryptedVoiceLedgerDatabase(
        @ApplicationContext context: Context,
        securityManager: SecurityManager
    ): VoiceLedgerDatabase {
        return buildEncryptedDatabase(context, securityManager)
    }
    
    private fun buildEncryptedDatabase(
        context: Context,
        securityManager: SecurityManager
    ): VoiceLedgerDatabase {
        val passphraseChars = securityManager.getDatabasePassphrase().toCharArray()
        return try {
            val passphraseBytes = SQLiteDatabase.getBytes(passphraseChars)
            Room.databaseBuilder(
                context.applicationContext,
                VoiceLedgerDatabase::class.java,
                VoiceLedgerDatabase.DATABASE_NAME
            )
                .openHelperFactory(SupportFactory(passphraseBytes))
                .addMigrations(*DatabaseMigrations.getAllMigrations())
                .addCallback(VoiceLedgerDatabase.createSeedDataCallback(context))
                .build()
        } finally {
            passphraseChars.fill('\u0000')
        }
    }
    
    // DAO Providers
    
    @Provides
    fun provideTransactionDao(database: VoiceLedgerDatabase): TransactionDao {
        return database.transactionDao()
    }
    
    @Provides
    fun provideDailySummaryDao(database: VoiceLedgerDatabase): DailySummaryDao {
        return database.dailySummaryDao()
    }
    
    @Provides
    fun provideSpeakerProfileDao(database: VoiceLedgerDatabase): SpeakerProfileDao {
        return database.speakerProfileDao()
    }
    
    @Provides
    fun provideProductVocabularyDao(database: VoiceLedgerDatabase): ProductVocabularyDao {
        return database.productVocabularyDao()
    }
    
    @Provides
    fun provideAudioMetadataDao(database: VoiceLedgerDatabase): AudioMetadataDao {
        return database.audioMetadataDao()
    }
    
    @Provides
    fun provideOfflineOperationDao(database: VoiceLedgerDatabase): OfflineOperationDao {
        return database.offlineOperationDao()
    }
    
    // Repository Providers
    
    @Provides
    @Singleton
    fun provideTransactionRepository(
        transactionDao: TransactionDao,
        securityManager: SecurityManager
    ): TransactionRepository {
        return TransactionRepositoryImpl(transactionDao, securityManager)
    }
    
    @Provides
    @Singleton
    fun provideDailySummaryRepository(
        dailySummaryDao: DailySummaryDao,
        transactionDao: TransactionDao
    ): DailySummaryRepository {
        return DailySummaryRepositoryImpl(dailySummaryDao, transactionDao)
    }
    
    @Provides
    @Singleton
    fun provideSpeakerProfileRepository(
        speakerProfileDao: SpeakerProfileDao
    ): SpeakerProfileRepository {
        return SpeakerProfileRepositoryImpl(speakerProfileDao)
    }
    
    @Provides
    @Singleton
    fun provideProductVocabularyRepository(
        productVocabularyDao: ProductVocabularyDao
    ): ProductVocabularyRepository {
        return ProductVocabularyRepositoryImpl(productVocabularyDao)
    }
    
    @Provides
    @Singleton
    fun provideAudioMetadataRepository(
        audioMetadataDao: AudioMetadataDao
    ): AudioMetadataRepository {
        return AudioMetadataRepositoryImpl(audioMetadataDao)
    }
    
    @Provides
    @Singleton
    fun provideTransactionAnalyticsRepository(
        transactionDao: TransactionDao,
        analyticsConsentProvider: AnalyticsConsentProvider
    ): TransactionAnalyticsRepository {
        return TransactionAnalyticsRepositoryImpl(transactionDao, analyticsConsentProvider)
    }
}