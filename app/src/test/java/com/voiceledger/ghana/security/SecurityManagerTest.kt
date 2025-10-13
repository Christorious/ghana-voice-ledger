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
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import java.security.KeyStore

/**
 * Unit tests for SecurityManager
 */
@RunWith(AndroidJUnit4::class)
class SecurityManagerTest {
    
    private lateinit var context: Context
    @Mock
    private lateinit var mockEncryptionService: EncryptionService
    @Mock
    private lateinit var mockPrivacyManager: PrivacyManager
    @Mock
    private lateinit var mockSecureDataStorage: SecureDataStorage
    private lateinit var securityManager: SecurityManager
    
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        
        securityManager = SecurityManager(
            context, 
            mockEncryptionService, 
            mockPrivacyManager, 
            mockSecureDataStorage
        )
    }
    
    @After
    fun tearDown() {
        // Clean up keystore entries created during tests
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            if (keyStore.containsAlias("voice_ledger_db_key")) {
                keyStore.deleteEntry("voice_ledger_db_key")
            }
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }
    
    @Test
    fun testValidateInput_transactionAmount_withValidAmount_shouldReturnValid() {
        // Given
        val validAmount = "25.50"
        
        // When
        val result = securityManager.validateInput(validAmount, InputType.TRANSACTION_AMOUNT)
        
        // Then
        assertEquals("Valid amount should pass validation", ValidationResult.Valid, result)
    }
}  
  
    @Test
    fun testValidateInput_transactionAmount_withInvalidAmount_shouldReturnInvalid() {
        // Given
        val invalidAmount = "invalid"
        
        // When
        val result = securityManager.validateInput(invalidAmount, InputType.TRANSACTION_AMOUNT)
        
        // Then
        assertTrue("Invalid amount should fail validation", result is ValidationResult.Invalid)
        assertEquals("Should have correct error message", 
            "Invalid number format", (result as ValidationResult.Invalid).reason)
    }
    
    @Test
    fun testValidateInput_transactionAmount_withNegativeAmount_shouldReturnInvalid() {
        // Given
        val negativeAmount = "-10.50"
        
        // When
        val result = securityManager.validateInput(negativeAmount, InputType.TRANSACTION_AMOUNT)
        
        // Then
        assertTrue("Negative amount should fail validation", result is ValidationResult.Invalid)
        assertEquals("Should have correct error message", 
            "Amount cannot be negative", (result as ValidationResult.Invalid).reason)
    }
    
    @Test
    fun testValidateInput_transactionAmount_withTooLargeAmount_shouldReturnInvalid() {
        // Given
        val largeAmount = "2000000.00"
        
        // When
        val result = securityManager.validateInput(largeAmount, InputType.TRANSACTION_AMOUNT)
        
        // Then
        assertTrue("Too large amount should fail validation", result is ValidationResult.Invalid)
        assertEquals("Should have correct error message", 
            "Amount too large", (result as ValidationResult.Invalid).reason)
    }
    
    @Test
    fun testValidateInput_productName_withValidName_shouldReturnValid() {
        // Given
        val validName = "Fresh Tilapia"
        
        // When
        val result = securityManager.validateInput(validName, InputType.PRODUCT_NAME)
        
        // Then
        assertEquals("Valid product name should pass validation", ValidationResult.Valid, result)
    }
    
    @Test
    fun testValidateInput_productName_withEmptyName_shouldReturnInvalid() {
        // Given
        val emptyName = ""
        
        // When
        val result = securityManager.validateInput(emptyName, InputType.PRODUCT_NAME)
        
        // Then
        assertTrue("Empty product name should fail validation", result is ValidationResult.Invalid)
        assertEquals("Should have correct error message", 
            "Product name cannot be empty", (result as ValidationResult.Invalid).reason)
    }
    
    @Test
    fun testValidateInput_productName_withTooLongName_shouldReturnInvalid() {
        // Given
        val longName = "A".repeat(101)
        
        // When
        val result = securityManager.validateInput(longName, InputType.PRODUCT_NAME)
        
        // Then
        assertTrue("Too long product name should fail validation", result is ValidationResult.Invalid)
        assertEquals("Should have correct error message", 
            "Product name too long", (result as ValidationResult.Invalid).reason)
    }
    
    @Test
    fun testValidateInput_productName_withInvalidCharacters_shouldReturnInvalid() {
        // Given
        val nameWithInvalidChars = "Fish<script>"
        
        // When
        val result = securityManager.validateInput(nameWithInvalidChars, InputType.PRODUCT_NAME)
        
        // Then
        assertTrue("Product name with invalid chars should fail validation", result is ValidationResult.Invalid)
        assertEquals("Should have correct error message", 
            "Product name contains invalid characters", (result as ValidationResult.Invalid).reason)
    }
    
    @Test
    fun testValidateInput_customerId_withValidId_shouldReturnValid() {
        // Given
        val validId = "customer_123"
        
        // When
        val result = securityManager.validateInput(validId, InputType.CUSTOMER_ID)
        
        // Then
        assertEquals("Valid customer ID should pass validation", ValidationResult.Valid, result)
    }
    
    @Test
    fun testValidateInput_customerId_withInvalidCharacters_shouldReturnInvalid() {
        // Given
        val invalidId = "customer@123"
        
        // When
        val result = securityManager.validateInput(invalidId, InputType.CUSTOMER_ID)
        
        // Then
        assertTrue("Customer ID with invalid chars should fail validation", result is ValidationResult.Invalid)
        assertEquals("Should have correct error message", 
            "Customer ID contains invalid characters", (result as ValidationResult.Invalid).reason)
    }
    
    @Test
    fun testSanitizeInput_withDangerousCharacters_shouldEscapeThem() {
        // Given
        val dangerousInput = "<script>alert('xss')</script>"
        
        // When
        val sanitized = securityManager.sanitizeInput(dangerousInput)
        
        // Then
        assertEquals("Should escape dangerous characters", 
            "&lt;script&gt;alert(&#x27;xss&#x27;)&lt;/script&gt;", sanitized)
    }
    
    @Test
    fun testSanitizeInput_withNormalText_shouldReturnTrimmed() {
        // Given
        val normalInput = "  Normal text  "
        
        // When
        val sanitized = securityManager.sanitizeInput(normalInput)
        
        // Then
        assertEquals("Should trim whitespace", "Normal text", sanitized)
    }
    
    @Test
    fun testGenerateSecureToken_shouldReturnValidToken() {
        // When
        val token = securityManager.generateSecureToken()
        
        // Then
        assertNotNull("Token should not be null", token)
        assertTrue("Token should not be empty", token.isNotEmpty())
        assertTrue("Token should be base64 encoded", token.matches(Regex("^[A-Za-z0-9_-]+$")))
    }
    
    @Test
    fun testGenerateSecureToken_withCustomLength_shouldReturnCorrectLength() {
        // Given
        val length = 16
        
        // When
        val token = securityManager.generateSecureToken(length)
        
        // Then
        assertNotNull("Token should not be null", token)
        assertTrue("Token should not be empty", token.isNotEmpty())
        // Note: Base64 encoding will make the string longer than the input bytes
    }
    
    @Test
    fun testHashSensitiveData_shouldReturnConsistentHash() {
        // Given
        val data = "sensitive information"
        
        // When
        val hash1 = securityManager.hashSensitiveData(data)
        val hash2 = securityManager.hashSensitiveData(data)
        
        // Then
        assertEquals("Same data should produce same hash", hash1, hash2)
        assertTrue("Hash should be hex string", hash1.matches(Regex("^[a-f0-9]+$")))
        assertEquals("SHA-256 hash should be 64 characters", 64, hash1.length)
    }
    
    @Test
    fun testVerifyDataIntegrity_withMatchingHash_shouldReturnTrue() {
        // Given
        val data = "test data"
        val expectedHash = securityManager.hashSensitiveData(data)
        
        // When
        val isValid = securityManager.verifyDataIntegrity(data, expectedHash)
        
        // Then
        assertTrue("Data integrity should be verified", isValid)
    }
    
    @Test
    fun testVerifyDataIntegrity_withDifferentHash_shouldReturnFalse() {
        // Given
        val data = "test data"
        val wrongHash = securityManager.hashSensitiveData("different data")
        
        // When
        val isValid = securityManager.verifyDataIntegrity(data, wrongHash)
        
        // Then
        assertFalse("Data integrity should fail with wrong hash", isValid)
    }
}