package com.voiceledger.ghana.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore

/**
 * Unit tests for EncryptionService
 */
@RunWith(AndroidJUnit4::class)
class EncryptionServiceTest {
    
    private lateinit var context: Context
    private lateinit var encryptionService: EncryptionService
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        encryptionService = EncryptionService(context)
    }
    
    @After
    fun tearDown() {
        // Clean up keystore entries created during tests
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            if (keyStore.containsAlias("VoiceLedgerMasterKey")) {
                keyStore.deleteEntry("VoiceLedgerMasterKey")
            }
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }
    
    @Test
    fun testEncryptAndDecrypt_withValidData_shouldSucceed() = runTest {
        // Given
        val plaintext = "Test sensitive data"
        
        // When
        val encryptedData = encryptionService.encrypt(plaintext)
        val decryptedText = encryptionService.decrypt(encryptedData)
        
        // Then
        assertEquals(plaintext, decryptedText)
        assertNotNull(encryptedData.encryptedData)
        assertNotNull(encryptedData.iv)
        assertTrue(encryptedData.encryptedData.isNotEmpty())
        assertTrue(encryptedData.iv.isNotEmpty())
    }
    
    @Test
    fun testEncrypt_withEmptyString_shouldSucceed() = runTest {
        // Given
        val plaintext = ""
        
        // When
        val encryptedData = encryptionService.encrypt(plaintext)
        val decryptedText = encryptionService.decrypt(encryptedData)
        
        // Then
        assertEquals(plaintext, decryptedText)
    }
    
    @Test
    fun testEncrypt_withLongText_shouldSucceed() = runTest {
        // Given
        val plaintext = "A".repeat(10000) // 10KB of text
        
        // When
        val encryptedData = encryptionService.encrypt(plaintext)
        val decryptedText = encryptionService.decrypt(encryptedData)
        
        // Then
        assertEquals(plaintext, decryptedText)
    }
    
    @Test
    fun testEncrypt_withSpecialCharacters_shouldSucceed() = runTest {
        // Given
        val plaintext = "Special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?`~"
        
        // When
        val encryptedData = encryptionService.encrypt(plaintext)
        val decryptedText = encryptionService.decrypt(encryptedData)
        
        // Then
        assertEquals(plaintext, decryptedText)
    }
    
    @Test
    fun testEncrypt_withUnicodeCharacters_shouldSucceed() = runTest {
        // Given
        val plaintext = "Unicode: 你好 🌍 العالم हैलो"
        
        // When
        val encryptedData = encryptionService.encrypt(plaintext)
        val decryptedText = encryptionService.decrypt(encryptedData)
        
        // Then
        assertEquals(plaintext, decryptedText)
    }
    
    @Test
    fun testEncrypt_multipleTimes_shouldProduceDifferentResults() = runTest {
        // Given
        val plaintext = "Same input text"
        
        // When
        val encryptedData1 = encryptionService.encrypt(plaintext)
        val encryptedData2 = encryptionService.encrypt(plaintext)
        
        // Then
        assertFalse("Encrypted data should be different due to random IV", 
            encryptedData1.encryptedData.contentEquals(encryptedData2.encryptedData))
        assertFalse("IV should be different", 
            encryptedData1.iv.contentEquals(encryptedData2.iv))
        
        // But both should decrypt to the same plaintext
        assertEquals(plaintext, encryptionService.decrypt(encryptedData1))
        assertEquals(plaintext, encryptionService.decrypt(encryptedData2))
    }
    
    @Test
    fun testEncryptWithKey_withCustomKey_shouldSucceed() = runTest {
        // Given
        val plaintext = "Test with custom key"
        val customKey = encryptionService.generateSecureKey(32)
        
        // When
        val encryptedData = encryptionService.encryptWithKey(plaintext, customKey)
        val decryptedText = encryptionService.decryptWithKey(encryptedData, customKey)
        
        // Then
        assertEquals(plaintext, decryptedText)
    }
    
    @Test(expected = EncryptionException::class)
    fun testDecryptWithKey_withWrongKey_shouldThrowException() = runTest {
        // Given
        val plaintext = "Test with wrong key"
        val correctKey = encryptionService.generateSecureKey(32)
        val wrongKey = encryptionService.generateSecureKey(32)
        
        // When
        val encryptedData = encryptionService.encryptWithKey(plaintext, correctKey)
        
        // Then - should throw exception
        encryptionService.decryptWithKey(encryptedData, wrongKey)
    }
    
    @Test
    fun testHashData_withSameInput_shouldProduceSameHash() = runTest {
        // Given
        val data = "Test data for hashing"
        val salt = encryptionService.generateSecureKey(16)
        
        // When
        val hash1 = encryptionService.hashData(data, salt)
        val hash2 = encryptionService.hashData(data, salt)
        
        // Then
        assertTrue("Hashes should be identical with same salt", 
            hash1.hash.contentEquals(hash2.hash))
        assertTrue("Salts should be identical", 
            hash1.salt.contentEquals(hash2.salt))
    }
    
    @Test
    fun testHashData_withDifferentSalt_shouldProduceDifferentHash() = runTest {
        // Given
        val data = "Test data for hashing"
        
        // When
        val hash1 = encryptionService.hashData(data) // Random salt
        val hash2 = encryptionService.hashData(data) // Different random salt
        
        // Then
        assertFalse("Hashes should be different with different salts", 
            hash1.hash.contentEquals(hash2.hash))
        assertFalse("Salts should be different", 
            hash1.salt.contentEquals(hash2.salt))
    }
    
    @Test
    fun testVerifyHash_withCorrectData_shouldReturnTrue() = runTest {
        // Given
        val data = "Test data for verification"
        val hashedData = encryptionService.hashData(data)
        
        // When
        val isValid = encryptionService.verifyHash(data, hashedData)
        
        // Then
        assertTrue("Hash verification should succeed", isValid)
    }
    
    @Test
    fun testVerifyHash_withIncorrectData_shouldReturnFalse() = runTest {
        // Given
        val originalData = "Original data"
        val modifiedData = "Modified data"
        val hashedData = encryptionService.hashData(originalData)
        
        // When
        val isValid = encryptionService.verifyHash(modifiedData, hashedData)
        
        // Then
        assertFalse("Hash verification should fail", isValid)
    }
    
    @Test
    fun testGenerateSecureKey_withDefaultSize_shouldGenerate32Bytes() {
        // When
        val key = encryptionService.generateSecureKey()
        
        // Then
        assertEquals("Default key size should be 32 bytes", 32, key.size)
    }
    
    @Test
    fun testGenerateSecureKey_withCustomSize_shouldGenerateCorrectSize() {
        // Given
        val keySize = 16
        
        // When
        val key = encryptionService.generateSecureKey(keySize)
        
        // Then
        assertEquals("Key size should match requested size", keySize, key.size)
    }
    
    @Test
    fun testGenerateSecureKey_multipleTimes_shouldProduceDifferentKeys() {
        // When
        val key1 = encryptionService.generateSecureKey()
        val key2 = encryptionService.generateSecureKey()
        
        // Then
        assertFalse("Generated keys should be different", 
            key1.contentEquals(key2))
    }
    
    @Test
    fun testSecureWipe_withByteArray_shouldZeroOutData() {
        // Given
        val data = byteArrayOf(1, 2, 3, 4, 5)
        val originalData = data.clone()
        
        // When
        encryptionService.secureWipe(data)
        
        // Then
        assertFalse("Data should be wiped", data.contentEquals(originalData))
        assertTrue("All bytes should be zero", data.all { it == 0.toByte() })
    }
    
    @Test
    fun testSecureWipe_withCharArray_shouldZeroOutData() {
        // Given
        val data = charArrayOf('a', 'b', 'c', 'd', 'e')
        val originalData = data.clone()
        
        // When
        encryptionService.secureWipe(data)
        
        // Then
        assertFalse("Data should be wiped", data.contentEquals(originalData))
        assertTrue("All chars should be null char", data.all { it == '\u0000' })
    }
    
    @Test
    fun testGetEncryptedSharedPreferences_shouldReturnValidPreferences() {
        // When
        val preferences = encryptionService.getEncryptedSharedPreferences("test_prefs")
        
        // Then
        assertNotNull("Encrypted preferences should not be null", preferences)
        
        // Test basic operations
        preferences.edit().putString("test_key", "test_value").apply()
        assertEquals("Should store and retrieve encrypted data", 
            "test_value", preferences.getString("test_key", null))
    }
    
    @Test
    fun testEncryptedData_equality_shouldWorkCorrectly() {
        // Given
        val data1 = byteArrayOf(1, 2, 3)
        val iv1 = byteArrayOf(4, 5, 6)
        val encryptedData1 = EncryptedData(data1, iv1)
        val encryptedData2 = EncryptedData(data1.clone(), iv1.clone())
        val encryptedData3 = EncryptedData(byteArrayOf(7, 8, 9), iv1)
        
        // Then
        assertEquals("Same data should be equal", encryptedData1, encryptedData2)
        assertNotEquals("Different data should not be equal", encryptedData1, encryptedData3)
        assertEquals("Hash codes should be equal for same data", 
            encryptedData1.hashCode(), encryptedData2.hashCode())
    }
    
    @Test
    fun testHashedData_equality_shouldWorkCorrectly() {
        // Given
        val hash1 = byteArrayOf(1, 2, 3)
        val salt1 = byteArrayOf(4, 5, 6)
        val hashedData1 = HashedData(hash1, salt1)
        val hashedData2 = HashedData(hash1.clone(), salt1.clone())
        val hashedData3 = HashedData(byteArrayOf(7, 8, 9), salt1)
        
        // Then
        assertEquals("Same data should be equal", hashedData1, hashedData2)
        assertNotEquals("Different data should not be equal", hashedData1, hashedData3)
        assertEquals("Hash codes should be equal for same data", 
            hashedData1.hashCode(), hashedData2.hashCode())
    }
}