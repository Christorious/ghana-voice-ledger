package com.voiceledger.ghana.security

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/** Ciphertext plus the random IV it was produced with. */
data class EncryptedData(
    val encryptedData: ByteArray,
    val iv: ByteArray
) {
    // ByteArray uses identity equality, so data-class equals must be written out.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EncryptedData) return false
        return encryptedData.contentEquals(other.encryptedData) && iv.contentEquals(other.iv)
    }

    override fun hashCode(): Int = 31 * encryptedData.contentHashCode() + iv.contentHashCode()
}

/**
 * AES-256-GCM encryption backed by the Android Keystore.
 *
 * The app-wide key never leaves the Keystore: [encrypt] and [decrypt] reference it
 * by alias. [encryptWithKey] and [decryptWithKey] take caller-supplied key material
 * instead, for data that must survive outside this install (database backups), where
 * the caller owns key custody.
 *
 * GCM is authenticated, so [decrypt] throws rather than returning wrong plaintext if
 * the ciphertext was tampered with. A fresh 12-byte IV is generated per operation and
 * stored alongside the ciphertext; an IV is not secret, but reusing one with the same
 * key breaks GCM, so never hard-code it.
 */
@Singleton
class EncryptionService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val APP_KEY_ALIAS = "voiceledger_app_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128
        private const val IV_BYTES = 12
        const val DEFAULT_KEY_LENGTH_BYTES = 32  // AES-256
    }

    /** Encrypt with the Keystore-held app key. */
    fun encrypt(data: String): EncryptedData {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, appKey())
        return EncryptedData(cipher.doFinal(data.toByteArray(Charsets.UTF_8)), cipher.iv)
    }

    /** Decrypt data produced by [encrypt]. Throws if the ciphertext fails authentication. */
    fun decrypt(data: EncryptedData): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, appKey(), GCMParameterSpec(GCM_TAG_BITS, data.iv))
        return String(cipher.doFinal(data.encryptedData), Charsets.UTF_8)
    }

    /** Encrypt with caller-supplied key material (backups, export files). */
    fun encryptWithKey(data: ByteArray, key: ByteArray): EncryptedData {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
        return EncryptedData(cipher.doFinal(data), cipher.iv)
    }

    /** Decrypt data produced by [encryptWithKey]. */
    fun decryptWithKey(data: EncryptedData, key: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, data.iv))
        return cipher.doFinal(data.encryptedData)
    }

    /** Fresh key material from [SecureRandom]. Never use a plain Random here. */
    fun generateSecureKey(lengthBytes: Int = DEFAULT_KEY_LENGTH_BYTES): ByteArray =
        ByteArray(lengthBytes).also { SecureRandom().nextBytes(it) }

    /** Preferences encrypted at rest with a Keystore-held master key. */
    fun getEncryptedSharedPreferences(name: String): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            name,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /** The app key, created on first use and held in the Keystore thereafter. */
    private fun appKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(APP_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                APP_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(DEFAULT_KEY_LENGTH_BYTES * 8)
                .build()
        )
        return generator.generateKey()
    }
}
