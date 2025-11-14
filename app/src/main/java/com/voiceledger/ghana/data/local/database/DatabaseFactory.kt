package com.voiceledger.ghana.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
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
 * Seed callback that delegates to VoiceLedgerDatabase's JSON-based seeding.
 * This ensures all seed data is externalized and version-controlled in assets/seed_data/
 */
internal fun createSeedDataCallback(context: Context): RoomDatabase.Callback {
    return VoiceLedgerDatabase.createSeedDataCallback(context)
}
