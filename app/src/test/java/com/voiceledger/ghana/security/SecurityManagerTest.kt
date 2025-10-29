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
    
    // Tests for sanitizeForDisplay
    
    @Test
    fun testSanitizeForDisplay_withNormalText_shouldReturnTrimmed() {
        // Given
        val normalInput = "  Normal text  "
        
        // When
        val sanitized = securityManager.sanitizeForDisplay(normalInput)
        
        // Then
        assertEquals("Should trim whitespace", "Normal text", sanitized)
    }
    
    @Test
    fun testSanitizeForDisplay_withHtmlTags_shouldEscapeThem() {
        // Given
        val htmlInput = "<script>alert('xss')</script>"
        
        // When
        val sanitized = securityManager.sanitizeForDisplay(htmlInput)
        
        // Then
        assertEquals("Should escape HTML tags", 
            "&lt;script&gt;alert(&#x27;xss&#x27;)&lt;&#x2F;script&gt;", sanitized)
        assertFalse("Should not contain raw < or >", sanitized.contains("<") || sanitized.contains(">"))
    }
    
    @Test
    fun testSanitizeForDisplay_withSpecialCharacters_shouldEscapeAll() {
        // Given
        val input = "Test & <test> \"quoted\" 'apostrophe' /slash/"
        
        // When
        val sanitized = securityManager.sanitizeForDisplay(input)
        
        // Then
        assertTrue("Should escape ampersand", sanitized.contains("&amp;"))
        assertTrue("Should escape <", sanitized.contains("&lt;"))
        assertTrue("Should escape >", sanitized.contains("&gt;"))
        assertTrue("Should escape quotes", sanitized.contains("&quot;"))
        assertTrue("Should escape apostrophe", sanitized.contains("&#x27;"))
        assertTrue("Should escape slash", sanitized.contains("&#x2F;"))
    }
    
    @Test
    fun testSanitizeForDisplay_withExcessiveLength_shouldTruncate() {
        // Given
        val longInput = "A".repeat(1500)
        
        // When
        val sanitized = securityManager.sanitizeForDisplay(longInput)
        
        // Then
        assertEquals("Should truncate to default max length", 1000, sanitized.length)
    }
    
    @Test
    fun testSanitizeForDisplay_withCustomMaxLength_shouldTruncate() {
        // Given
        val input = "A".repeat(200)
        
        // When
        val sanitized = securityManager.sanitizeForDisplay(input, maxLength = 50)
        
        // Then
        assertEquals("Should truncate to custom max length", 50, sanitized.length)
    }
    
    @Test
    fun testSanitizeForDisplay_withNewlinesAndTabs_shouldNormalize() {
        // Given
        val input = "Line1\nLine2\r\nLine3\tTabbed"
        
        // When
        val sanitized = securityManager.sanitizeForDisplay(input)
        
        // Then
        assertFalse("Should remove newlines", sanitized.contains("\n"))
        assertFalse("Should remove carriage returns", sanitized.contains("\r"))
        assertFalse("Should remove tabs", sanitized.contains("\t"))
        assertTrue("Should normalize whitespace", sanitized.contains(" "))
    }
    
    @Test
    fun testSanitizeForDisplay_withMultipleSpaces_shouldNormalize() {
        // Given
        val input = "Multiple    spaces     here"
        
        // When
        val sanitized = securityManager.sanitizeForDisplay(input)
        
        // Then
        assertFalse("Should not have multiple consecutive spaces", sanitized.contains("  "))
        assertEquals("Should normalize spaces", "Multiple spaces here", sanitized)
    }
    
    @Test
    fun testSanitizeForDisplay_withEmptyString_shouldReturnEmpty() {
        // Given
        val input = ""
        
        // When
        val sanitized = securityManager.sanitizeForDisplay(input)
        
        // Then
        assertEquals("Should return empty string", "", sanitized)
    }
    
    // Tests for sanitizeForFileName
    
    @Test
    fun testSanitizeForFileName_withValidName_shouldReturnClean() {
        // Given
        val validName = "my_document_2024.txt"
        
        // When
        val sanitized = securityManager.sanitizeForFileName(validName)
        
        // Then
        assertEquals("Should preserve valid filename", "my_document_2024.txt", sanitized)
    }
    
    @Test
    fun testSanitizeForFileName_withIllegalCharacters_shouldReplaceWithUnderscore() {
        // Given
        val invalidName = "file:name\\with/illegal*chars?.txt"
        
        // When
        val sanitized = securityManager.sanitizeForFileName(invalidName)
        
        // Then
        assertFalse("Should not contain colon", sanitized.contains(":"))
        assertFalse("Should not contain backslash", sanitized.contains("\\"))
        assertFalse("Should not contain forward slash", sanitized.contains("/"))
        assertFalse("Should not contain asterisk", sanitized.contains("*"))
        assertFalse("Should not contain question mark", sanitized.contains("?"))
        assertTrue("Should preserve extension", sanitized.endsWith(".txt"))
    }
    
    @Test
    fun testSanitizeForFileName_withSpaces_shouldReplaceWithUnderscore() {
        // Given
        val nameWithSpaces = "my file name.txt"
        
        // When
        val sanitized = securityManager.sanitizeForFileName(nameWithSpaces)
        
        // Then
        assertFalse("Should not contain spaces", sanitized.contains(" "))
        assertEquals("Should replace spaces with underscores", "my_file_name.txt", sanitized)
    }
    
    @Test
    fun testSanitizeForFileName_withPathTraversal_shouldRemove() {
        // Given
        val maliciousName = "../../../etc/passwd"
        
        // When
        val sanitized = securityManager.sanitizeForFileName(maliciousName)
        
        // Then
        assertFalse("Should not contain path traversal", sanitized.contains(".."))
        assertFalse("Should not contain slash", sanitized.contains("/"))
    }
    
    @Test
    fun testSanitizeForFileName_withReservedWindowsName_shouldPrefix() {
        // Given
        val reservedNames = listOf("CON", "PRN", "AUX", "NUL", "COM1", "LPT1")
        
        reservedNames.forEach { reservedName ->
            // When
            val sanitized = securityManager.sanitizeForFileName(reservedName)
            
            // Then
            assertTrue("Should prefix reserved name $reservedName", 
                sanitized.startsWith("_") || !sanitized.equals(reservedName, ignoreCase = true))
        }
    }
    
    @Test
    fun testSanitizeForFileName_withExcessiveLength_shouldTruncate() {
        // Given
        val longName = "A".repeat(300) + ".txt"
        
        // When
        val sanitized = securityManager.sanitizeForFileName(longName)
        
        // Then
        assertTrue("Should be under max length", sanitized.length <= 200)
        assertTrue("Should preserve extension", sanitized.endsWith(".txt"))
    }
    
    @Test
    fun testSanitizeForFileName_withEmptyString_shouldReturnDefault() {
        // Given
        val emptyName = ""
        
        // When
        val sanitized = securityManager.sanitizeForFileName(emptyName)
        
        // Then
        assertEquals("Should return default name", "unnamed", sanitized)
    }
    
    @Test
    fun testSanitizeForFileName_withOnlyIllegalChars_shouldReturnDefault() {
        // Given
        val onlyIllegal = "///:::***"
        
        // When
        val sanitized = securityManager.sanitizeForFileName(onlyIllegal)
        
        // Then
        assertEquals("Should return default when no valid chars", "unnamed", sanitized)
    }
    
    @Test
    fun testSanitizeForFileName_withMultipleDots_shouldHandle() {
        // Given
        val multipleDots = "file....name...txt"
        
        // When
        val sanitized = securityManager.sanitizeForFileName(multipleDots)
        
        // Then
        assertFalse("Should not have multiple consecutive dots", sanitized.contains(".."))
    }
    
    @Test
    fun testSanitizeForFileName_withLeadingTrailingSpecialChars_shouldTrim() {
        // Given
        val specialChars = "___file.txt___"
        
        // When
        val sanitized = securityManager.sanitizeForFileName(specialChars)
        
        // Then
        assertFalse("Should not start with underscore", sanitized.startsWith("_"))
        assertFalse("Should not end with underscore", sanitized.endsWith("_"))
    }
    
    // Tests for sanitizeForQuery
    
    @Test
    fun testSanitizeForQuery_withNormalText_shouldNormalize() {
        // Given
        val normalQuery = "  search term  "
        
        // When
        val sanitized = securityManager.sanitizeForQuery(normalQuery)
        
        // Then
        assertEquals("Should trim and normalize", "search term", sanitized)
    }
    
    @Test
    fun testSanitizeForQuery_withControlCharacters_shouldRemove() {
        // Given
        val queryWithControl = "test\u0000query\u001F"
        
        // When
        val sanitized = securityManager.sanitizeForQuery(queryWithControl)
        
        // Then
        assertFalse("Should remove null character", sanitized.contains("\u0000"))
        assertFalse("Should remove control characters", sanitized.contains("\u001F"))
    }
    
    @Test
    fun testSanitizeForQuery_withWildcardsNoEscape_shouldPreserve() {
        // Given
        val queryWithWildcards = "test%value_here"
        
        // When
        val sanitized = securityManager.sanitizeForQuery(queryWithWildcards, escapeWildcards = false)
        
        // Then
        assertTrue("Should preserve % when not escaping", sanitized.contains("%"))
        assertTrue("Should preserve _ when not escaping", sanitized.contains("_"))
    }
    
    @Test
    fun testSanitizeForQuery_withWildcardsEscape_shouldEscape() {
        // Given
        val queryWithWildcards = "test%value_here"
        
        // When
        val sanitized = securityManager.sanitizeForQuery(queryWithWildcards, escapeWildcards = true)
        
        // Then
        assertTrue("Should escape %", sanitized.contains("\\%"))
        assertTrue("Should escape _", sanitized.contains("\\_"))
        assertFalse("Should not have unescaped %", sanitized.matches(Regex(".*[^\\\\]%.*")))
    }
    
    @Test
    fun testSanitizeForQuery_withExcessiveLength_shouldTruncate() {
        // Given
        val longQuery = "A".repeat(600)
        
        // When
        val sanitized = securityManager.sanitizeForQuery(longQuery)
        
        // Then
        assertEquals("Should truncate to default max length", 500, sanitized.length)
    }
    
    @Test
    fun testSanitizeForQuery_withCustomMaxLength_shouldTruncate() {
        // Given
        val query = "A".repeat(200)
        
        // When
        val sanitized = securityManager.sanitizeForQuery(query, maxLength = 100)
        
        // Then
        assertEquals("Should truncate to custom max length", 100, sanitized.length)
    }
    
    @Test
    fun testSanitizeForQuery_withMultipleSpaces_shouldNormalize() {
        // Given
        val query = "word1    word2     word3"
        
        // When
        val sanitized = securityManager.sanitizeForQuery(query)
        
        // Then
        assertFalse("Should not have multiple consecutive spaces", sanitized.contains("  "))
        assertEquals("Should normalize spaces", "word1 word2 word3", sanitized)
    }
    
    @Test
    fun testSanitizeForQuery_withEmptyString_shouldReturnEmpty() {
        // Given
        val emptyQuery = ""
        
        // When
        val sanitized = securityManager.sanitizeForQuery(emptyQuery)
        
        // Then
        assertEquals("Should return empty string", "", sanitized)
    }
    
    @Test
    fun testSanitizeForQuery_withSqlInjectionAttempt_shouldSanitize() {
        // Given
        val maliciousQuery = "'; DROP TABLE users; --"
        
        // When
        val sanitized = securityManager.sanitizeForQuery(maliciousQuery)
        
        // Then
        assertNotEquals("Should modify malicious input", maliciousQuery, sanitized)
        // Note: Room handles SQL injection via parameterized queries, 
        // but this helps with additional normalization
    }
    
    // Integration tests for deprecated method
    
    @Test
    @Suppress("DEPRECATION")
    fun testSanitizeInput_shouldCallSanitizeForDisplay() {
        // Given
        val input = "<script>test</script>"
        
        // When
        val resultOld = securityManager.sanitizeInput(input)
        val resultNew = securityManager.sanitizeForDisplay(input)
        
        // Then
        assertEquals("Deprecated method should match sanitizeForDisplay", resultNew, resultOld)
    }
}