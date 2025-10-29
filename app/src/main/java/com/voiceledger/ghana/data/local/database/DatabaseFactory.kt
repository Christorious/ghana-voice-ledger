package com.voiceledger.ghana.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import net.sqlcipher.room.SupportFactory

/**
 * Centralized factory for creating VoiceLedgerDatabase instances.
 * Handles both encrypted and non-encrypted database configurations while ensuring
 * a consistent builder pipeline.
 */
object DatabaseFactory {

    /**
     * Create a new [VoiceLedgerDatabase] instance.
     *
     * @param context Application context required for builder creation and callbacks
     * @param encrypted When true, SQLCipher encryption will be applied using [passphrase]
     * @param passphrase Encryption passphrase (required when [encrypted] is true)
     * @param additionalCallbacks Optional extra callbacks to append to the builder
     */
    fun createDatabase(
        context: Context,
        encrypted: Boolean,
        passphrase: String? = null,
        additionalCallbacks: List<RoomDatabase.Callback> = emptyList()
    ): VoiceLedgerDatabase {
        val builder = Room.databaseBuilder(
            context.applicationContext,
            VoiceLedgerDatabase::class.java,
            VoiceLedgerDatabase.DATABASE_NAME
        )

        if (encrypted) {
            requireNotNull(passphrase) { "Passphrase is required for encrypted database creation" }
            val factory = SupportFactory(passphrase.toByteArray())
            builder.openHelperFactory(factory)
        }

        val callbacks = buildList {
            add(createSeedDataCallback(context))
            addAll(additionalCallbacks)
        }

        callbacks.forEach(builder::addCallback)

        return builder
            .addMigrations(*DatabaseMigrations.getAllMigrations())
            .build()
    }
}

/**
 * Seed callback that inserts initial data required by the application. The context is supplied
 * to support future enhancements that may need localized or resource-based seed data.
 */
internal fun createSeedDataCallback(context: Context): RoomDatabase.Callback {
    return object : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            populateInitialData(db)
        }

        private fun populateInitialData(db: SupportSQLiteDatabase) {
            // Insert common Ghana fish products
            val fishProducts = listOf(
                // Tilapia variants
                "('tilapia-001', 'Tilapia', 'fish', 'tilapia,apateshi,tuo', 12.0, 25.0, 'piece,bowl', 0, 1, NULL, 'apateshi,tuo', NULL, ${System.currentTimeMillis()}, ${System.currentTimeMillis()}, 0, 1.0)",

                // Mackerel variants  
                "('mackerel-001', 'Mackerel', 'fish', 'mackerel,kpanla,titus', 8.0, 18.0, 'piece,tin', 0, 1, NULL, 'kpanla', NULL, ${System.currentTimeMillis()}, ${System.currentTimeMillis()}, 0, 1.0)",

                // Sardines variants
                "('sardines-001', 'Sardines', 'fish', 'sardines,herring,sardin', 5.0, 12.0, 'tin,piece', 0, 1, NULL, 'herring', NULL, ${System.currentTimeMillis()}, ${System.currentTimeMillis()}, 0, 1.0)",

                // Tuna variants
                "('tuna-001', 'Tuna', 'fish', 'tuna,light meat,chunk light', 15.0, 30.0, 'tin,piece', 0, 1, NULL, NULL, NULL, ${System.currentTimeMillis()}, ${System.currentTimeMillis()}, 0, 1.0)",

                // Red fish (Red Snapper)
                "('redfish-001', 'Red Fish', 'fish', 'red fish,red snapper,adwene', 20.0, 45.0, 'piece,size', 0, 1, NULL, 'adwene', NULL, ${System.currentTimeMillis()}, ${System.currentTimeMillis()}, 0, 1.0)",

                // Salmon variants
                "('salmon-001', 'Salmon', 'fish', 'salmon,pink salmon', 25.0, 50.0, 'piece,tin', 0, 1, NULL, NULL, NULL, ${System.currentTimeMillis()}, ${System.currentTimeMillis()}, 0, 1.0)",

                // Catfish variants
                "('catfish-001', 'Catfish', 'fish', 'catfish,mudfish,sumbre', 18.0, 35.0, 'piece,size', 0, 1, NULL, 'sumbre', NULL, ${System.currentTimeMillis()}, ${System.currentTimeMillis()}, 0, 1.0)",

                // Croaker variants
                "('croaker-001', 'Croaker', 'fish', 'croaker,komi,yellow croaker', 10.0, 22.0, 'piece,size', 0, 1, NULL, 'komi', NULL, ${System.currentTimeMillis()}, ${System.currentTimeMillis()}, 0, 1.0)"
            )

            fishProducts.forEach { product ->
                db.execSQL("""
                    INSERT INTO product_vocabulary 
                    (id, canonicalName, category, variants, minPrice, maxPrice, measurementUnits, frequency, isActive, seasonality, twiNames, gaNames, createdAt, updatedAt, isLearned, learningConfidence)
                    VALUES $product
                """)
            }

            // Insert common measurement units and currency terms
            val measurementProducts = listOf(
                "('bowl-001', 'Bowl', 'measurement', 'bowl,kokoo,rubber', 0.0, 0.0, 'bowl', 0, 1, NULL, 'kokoo', NULL, ${System.currentTimeMillis()}, ${System.currentTimeMillis()}, 0, 1.0)",
                "('bucket-001', 'Bucket', 'measurement', 'bucket,rubber,container', 0.0, 0.0, 'bucket', 0, 1, NULL, 'rubber', NULL, ${System.currentTimeMillis()}, ${System.currentTimeMillis()}, 0, 1.0)",
                "('piece-001', 'Piece', 'measurement', 'piece,one,single', 0.0, 0.0, 'piece', 0, 1, NULL, NULL, NULL, ${System.currentTimeMillis()}, ${System.currentTimeMillis()}, 0, 1.0)"
            )

            measurementProducts.forEach { product ->
                db.execSQL("""
                    INSERT INTO product_vocabulary 
                    (id, canonicalName, category, variants, minPrice, maxPrice, measurementUnits, frequency, isActive, seasonality, twiNames, gaNames, createdAt, updatedAt, isLearned, learningConfidence)
                    VALUES $product
                """)
            }
        }
    }
}
