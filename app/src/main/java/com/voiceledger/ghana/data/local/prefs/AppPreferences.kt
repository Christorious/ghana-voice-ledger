package com.voiceledger.ghana.data.local.prefs

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plain (unencrypted) preferences for non-sensitive app state: which tutorials a
 * trader has seen, whether onboarding is done, whether she has enrolled her voice.
 *
 * Anything about her sales, her customers or her voiceprint belongs in
 * [com.voiceledger.ghana.security.EncryptionService]'s encrypted preferences or in
 * the encrypted database — not here.
 */
@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("voiceledger_app_state", Context.MODE_PRIVATE)

    fun getBoolean(key: String, default: Boolean = false): Boolean = prefs.getBoolean(key, default)
    fun putBoolean(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()

    fun getInt(key: String, default: Int = 0): Int = prefs.getInt(key, default)
    fun putInt(key: String, value: Int) = prefs.edit().putInt(key, value).apply()

    fun getLong(key: String, default: Long = 0L): Long = prefs.getLong(key, default)
    fun putLong(key: String, value: Long) = prefs.edit().putLong(key, value).apply()

    fun getString(key: String, default: String? = null): String? = prefs.getString(key, default)
    fun putString(key: String, value: String) = prefs.edit().putString(key, value).apply()
}
