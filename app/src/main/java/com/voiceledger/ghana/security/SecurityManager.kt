package com.voiceledger.ghana.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central security manager for encryption, key management, and data protection
 * Handles database encryption keys, API key security, and cryptographic operations
 */
@Singleton
class SecurityManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryptionService: EncryptionService,
    private val privacyManager: PrivacyManager,
    private val secureDataStorage: SecureDataStorage
) {
    
    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    }
    
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }
    
    private val encryptedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            "voice_ledger_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    companion object {
        private const val DATABASE_KEY_ALIAS = "voice_ledger_db_key"
        private const val API_KEY_ALIAS = "voice_ledger_api_key"
        private const val DATABASE_PASSPHRASE_PREF_KEY = "database_passphrase"
        private const val DATABASE_PASSPHRASE_LENGTH_BYTES = 32
        private const val ENCRYPTION_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 16
    }
    
    /**
     * Initialize security components
     */
    suspend fun initialize() {
        ensureDatabaseKey()
        withContext(Dispatchers.IO) {
            initializeApiKeyStorage()
        }
    }
    
    /**
     * Generate database encryption key
     */
    private fun generateDatabaseKey() {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            DATABASE_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false) // For background operation
            .build()
        
        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }
    
    private fun generateDatabasePassphrase(): String {
        val randomBytes = ByteArray(DATABASE_PASSPHRASE_LENGTH_BYTES)
        SecureRandom().nextBytes(randomBytes)
        val passphrase = randomBytes.joinToString("") { "%02x".format(it) }
        randomBytes.fill(0)
        return passphrase
    }
    
    suspend fun ensureDatabaseKey() {
        withContext(Dispatchers.IO) {
            val hasAlias = try {
                keyStore.containsAlias(DATABASE_KEY_ALIAS)
            } catch (e: Exception) {
                false
            }
            if (!hasAlias) {
                generateDatabaseKey()
            } else if (getDatabaseKey() == null) {
                try {
                    keyStore.deleteEntry(DATABASE_KEY_ALIAS)
                } catch (_: Exception) {
                    // Ignore deletion failures; we'll attempt to regenerate the key regardless
                }
                generateDatabaseKey()
            }
            
            val existingPassphrase = encryptedPreferences.getString(DATABASE_PASSPHRASE_PREF_KEY, null)
            if (existingPassphrase.isNullOrEmpty()) {
                val passphrase = generateDatabasePassphrase()
                val success = encryptedPreferences.edit()
                    .putString(DATABASE_PASSPHRASE_PREF_KEY, passphrase)
                    .commit()
                if (!success) {
                    throw SecurityException("Failed to persist database passphrase")
                }
            }
        }
    }
    
    /**
     * Get database encryption key
     */
    fun getDatabaseKey(): SecretKey? {
        return try {
            keyStore.getKey(DATABASE_KEY_ALIAS, null) as? SecretKey
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get database passphrase for SQLCipher
     */
    fun getDatabasePassphrase(): String {
        val passphrase = encryptedPreferences.getString(DATABASE_PASSPHRASE_PREF_KEY, null)
        if (passphrase.isNullOrEmpty()) {
            throw SecurityException("Database passphrase not available. Call ensureDatabaseKey() before accessing the database.")
        }
        return passphrase
    }
    
    /**
     * Store API key securely
     */
    suspend fun storeApiKey(keyName: String, apiKey: String) {
        withContext(Dispatchers.IO) {
            try {
                val encryptedKey = encryptData(apiKey.toByteArray())
                encryptedPreferences.edit()
                    .putString("api_key_$keyName", encryptedKey)
                    .apply()
            } catch (e: Exception) {
                throw SecurityException("Failed to store API key: ${e.message}", e)
            }
        }
    }
    
    /**
     * Retrieve API key securely
     */
    suspend fun getApiKey(keyName: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val encryptedKey = encryptedPreferences.getString("api_key_$keyName", null)
                if (encryptedKey != null) {
                    val decryptedBytes = decryptData(encryptedKey)
                    String(decryptedBytes)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Encrypt data using Android Keystore
     */
    private fun encryptData(data: ByteArray): String {
        val secretKey = getDatabaseKey() ?: throw SecurityException("Database key not available")
        
        val cipher = Cipher.getInstance(ENCRYPTION_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        
        val iv = cipher.iv
        val encryptedData = cipher.doFinal(data)
        
        // Combine IV and encrypted data
        val combined = ByteArray(iv.size + encryptedData.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encryptedData, 0, combined, iv.size, encryptedData.size)
        
        return android.util.Base64.encodeToString(combined, android.util.Base64.DEFAULT)
    }
    
    /**
     * Decrypt data using Android Keystore
     */
    private fun decryptData(encryptedData: String): ByteArray {
        val secretKey = getDatabaseKey() ?: throw SecurityException("Database key not available")
        
        val combined = android.util.Base64.decode(encryptedData, android.util.Base64.DEFAULT)
        
        // Extract IV and encrypted data
        val iv = ByteArray(GCM_IV_LENGTH)
        val encrypted = ByteArray(combined.size - GCM_IV_LENGTH)
        System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)
        System.arraycopy(combined, GCM_IV_LENGTH, encrypted, 0, encrypted.size)
        
        val cipher = Cipher.getInstance(ENCRYPTION_TRANSFORMATION)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
        
        return cipher.doFinal(encrypted)
    }
    
    /**
     * Initialize API key storage
     */
    private fun initializeApiKeyStorage() {
        // Ensure encrypted preferences are accessible
        try {
            encryptedPreferences.edit().putString("init_test", "test").apply()
            encryptedPreferences.edit().remove("init_test").apply()
        } catch (e: Exception) {
            throw SecurityException("Failed to initialize secure storage: ${e.message}", e)
        }
    }
    
    /**
     * Generate secure random token
     */
    fun generateSecureToken(length: Int = 32): String {
        val secureRandom = SecureRandom()
        val bytes = ByteArray(length)
        secureRandom.nextBytes(bytes)
        return android.util.Base64.encodeToString(bytes, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
    }
    
    /**
     * Hash sensitive data (for logging or comparison)
     */
    fun hashSensitiveData(data: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(data.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Validate input data for security
     */
    fun validateInput(input: String, type: InputType): ValidationResult {
        return when (type) {
            InputType.TRANSACTION_AMOUNT -> validateTransactionAmount(input)
            InputType.PRODUCT_NAME -> validateProductName(input)
            InputType.CUSTOMER_ID -> validateCustomerId(input)
            InputType.SPEAKER_ID -> validateSpeakerId(input)
            InputType.GENERAL_TEXT -> validateGeneralText(input)
        }
    }
    
    /**
     * Validate transaction amount
     */
    private fun validateTransactionAmount(amount: String): ValidationResult {
        return try {
            val value = amount.toDoubleOrNull()
            when {
                value == null -> ValidationResult.Invalid("Invalid number format")
                value < 0 -> ValidationResult.Invalid("Amount cannot be negative")
                value > 1_000_000 -> ValidationResult.Invalid("Amount too large")
                else -> ValidationResult.Valid
            }
        } catch (e: Exception) {
            ValidationResult.Invalid("Invalid amount format")
        }
    }
    
    /**
     * Validate product name
     */
    private fun validateProductName(name: String): ValidationResult {
        return when {
            name.isBlank() -> ValidationResult.Invalid("Product name cannot be empty")
            name.length > 100 -> ValidationResult.Invalid("Product name too long")
            name.contains(Regex("[<>\"'&]")) -> ValidationResult.Invalid("Product name contains invalid characters")
            else -> ValidationResult.Valid
        }
    }
    
    /**
     * Validate customer ID
     */
    private fun validateCustomerId(id: String): ValidationResult {
        return when {
            id.isBlank() -> ValidationResult.Invalid("Customer ID cannot be empty")
            id.length > 50 -> ValidationResult.Invalid("Customer ID too long")
            !id.matches(Regex("^[a-zA-Z0-9_-]+$")) -> ValidationResult.Invalid("Customer ID contains invalid characters")
            else -> ValidationResult.Valid
        }
    }
    
    /**
     * Validate speaker ID
     */
    private fun validateSpeakerId(id: String): ValidationResult {
        return when {
            id.isBlank() -> ValidationResult.Invalid("Speaker ID cannot be empty")
            id.length > 50 -> ValidationResult.Invalid("Speaker ID too long")
            !id.matches(Regex("^[a-zA-Z0-9_-]+$")) -> ValidationResult.Invalid("Speaker ID contains invalid characters")
            else -> ValidationResult.Valid
        }
    }
    
    /**
     * Validate general text input
     */
    private fun validateGeneralText(text: String): ValidationResult {
        return when {
            text.length > 1000 -> ValidationResult.Invalid("Text too long")
            text.contains(Regex("[<>\"'&;]")) -> ValidationResult.Invalid("Text contains potentially dangerous characters")
            else -> ValidationResult.Valid
        }
    }
    
    /**
     * Sanitize input by removing or escaping dangerous characters
     * @deprecated Use context-specific sanitization methods instead
     * @see sanitizeForDisplay
     * @see sanitizeForFileName
     * @see sanitizeForQuery
     */
    @Deprecated("Use sanitizeForDisplay, sanitizeForFileName, or sanitizeForQuery instead")
    fun sanitizeInput(input: String): String {
        return sanitizeForDisplay(input)
    }
    
    /**
     * Sanitize input for safe display in UI
     * Escapes HTML/XML special characters to prevent XSS attacks
     * 
     * @param input The user-provided string to sanitize
     * @param maxLength Maximum allowed length (default: 1000 characters)
     * @return Sanitized string safe for display
     */
    fun sanitizeForDisplay(input: String, maxLength: Int = 1000): String {
        if (input.isEmpty()) return input
        
        val truncated = if (input.length > maxLength) {
            input.substring(0, maxLength)
        } else {
            input
        }
        
        return truncated
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .replace("/", "&#x2F;")
            .replace("\n", " ")
            .replace("\r", "")
            .replace("\t", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
    
    /**
     * Sanitize input for safe use in filenames
     * Removes or replaces characters that could cause filesystem issues or path traversal attacks
     * 
     * @param input The filename to sanitize
     * @param maxLength Maximum allowed length (default: 200 characters)
     * @return Sanitized filename safe for filesystem operations
     */
    fun sanitizeForFileName(input: String, maxLength: Int = 200): String {
        if (input.isEmpty()) return "unnamed"
        
        val reservedNames = setOf(
            "CON", "PRN", "AUX", "NUL",
            "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
            "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
        )
        
        var sanitized = input
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace(Regex("\\.\\.+"), "_")
            .replace(" ", "_")
            .replace(Regex("[^a-zA-Z0-9._-]"), "")
            .replace(Regex("_{2,}"), "_")
            .trim('_')
            .trim('.')
        
        if (sanitized.isEmpty()) {
            sanitized = "unnamed"
        }
        
        val nameWithoutExt = sanitized.substringBeforeLast('.')
        val extension = if (sanitized.contains('.')) {
            "." + sanitized.substringAfterLast('.').take(10)
        } else {
            ""
        }
        
        val baseName = nameWithoutExt.take(maxLength - extension.length)
        sanitized = baseName + extension
        
        val upperName = sanitized.uppercase().substringBefore('.')
        if (upperName in reservedNames) {
            sanitized = "_$sanitized"
        }
        
        return sanitized
    }
    
    /**
     * Sanitize input for safe use in database queries
     * While Room handles SQL injection via parameterized queries, this helps with:
     * - LIKE query wildcards
     * - Full-text search special characters
     * - Whitespace normalization
     * 
     * @param input The query text to sanitize
     * @param maxLength Maximum allowed length (default: 500 characters)
     * @param escapeWildcards Whether to escape SQL LIKE wildcards (% and _)
     * @return Sanitized string safe for query operations
     */
    fun sanitizeForQuery(input: String, maxLength: Int = 500, escapeWildcards: Boolean = false): String {
        if (input.isEmpty()) return input
        
        val truncated = if (input.length > maxLength) {
            input.substring(0, maxLength)
        } else {
            input
        }
        
        var sanitized = truncated
            .replace(Regex("[\\x00-\\x1F\\x7F]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        
        if (escapeWildcards) {
            sanitized = sanitized
                .replace("%", "\\%")
                .replace("_", "\\_")
        }
        
        return sanitized
    }
    
    /**
     * Sanitize input for display in UI components
     * Removes HTML tags and escapes special characters for safe display
     */
    fun sanitizeForDisplay(input: String): String {
        return input
            .replace(Regex("<[^>]*>"), "") // Remove HTML tags
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .replace("&", "&amp;")
            .replace("\n", " ")
            .replace("\r", " ")
            .replace("\t", " ")
            .trim()
            .replace(Regex("\\s+"), " ") // Normalize whitespace
    }
    
    /**
     * Sanitize input for use in file names
     * Removes dangerous characters and replaces spaces with underscores
     */
    fun sanitizeForFileName(input: String): String {
        return input
            .replace(Regex("[<>:\"/\\\\|?*]"), "") // Remove invalid filename characters
            .replace(" ", "_")
            .replace("&", "_")
            .replace(";", "_")
            .replace("'", "_")
            .replace("\"", "_")
            .replace(Regex("_+"), "_") // Replace multiple underscores with single
            .replace(Regex("^_|_$"), "") // Remove leading/trailing underscores
            .trim()
            .take(255) // Limit filename length
    }
    
    /**
     * Sanitize input for use in database queries
     * Removes SQL injection risk characters and limits length
     */
    fun sanitizeForQuery(input: String): String {
        return input
            .replace("'", "''") // Escape single quotes for SQL
            .replace("\"", "\"\"") // Escape double quotes
            .replace("\\", "\\\\") // Escape backslashes
            .replace("%", "\\%") // Escape wildcard characters
            .replace("_", "\\_")
            .trim()
            .take(1000) // Reasonable limit for query parameters
    }
    
    /**
     * Check if device has secure lock screen
     */
    fun hasSecureLockScreen(): Boolean {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
        return keyguardManager.isDeviceSecure
    }
    
    /**
     * Get security recommendations
     */
    fun getSecurityRecommendations(): List<SecurityRecommendation> {
        val recommendations = mutableListOf<SecurityRecommendation>()
        
        if (!hasSecureLockScreen()) {
            recommendations.add(
                SecurityRecommendation(
                    type = SecurityRecommendationType.DEVICE_SECURITY,
                    severity = SecuritySeverity.HIGH,
                    title = "No Secure Lock Screen",
                    description = "Device does not have a secure lock screen (PIN, password, pattern, or biometric)",
                    recommendation = "Enable a secure lock screen to protect your business data"
                )
            )
        }
        
        // Check if database key exists
        if (getDatabaseKey() == null) {
            recommendations.add(
                SecurityRecommendation(
                    type = SecurityRecommendationType.ENCRYPTION,
                    severity = SecuritySeverity.CRITICAL,
                    title = "Database Encryption Key Missing",
                    description = "Database encryption key is not available",
                    recommendation = "Restart the app to regenerate encryption keys"
                )
            )
        }
        
        return recommendations
    }
    
    /**
     * Clear all sensitive data
     */
    suspend fun clearAllSensitiveData() {
        withContext(Dispatchers.IO) {
            try {
                // Clear encrypted preferences
                encryptedPreferences.edit().clear().apply()
                
                // Clear keystore entries
                if (keyStore.containsAlias(DATABASE_KEY_ALIAS)) {
                    keyStore.deleteEntry(DATABASE_KEY_ALIAS)
                }
                
                if (keyStore.containsAlias(API_KEY_ALIAS)) {
                    keyStore.deleteEntry(API_KEY_ALIAS)
                }
                
            } catch (e: Exception) {
                throw SecurityException("Failed to clear sensitive data: ${e.message}", e)
            }
        }
    }
    
    /**
     * Verify data integrity
     */
    fun verifyDataIntegrity(data: String, expectedHash: String): Boolean {
        val actualHash = hashSensitiveData(data)
        return actualHash == expectedHash
    }
    
    /**
     * Get security status
     */
    fun getSecurityStatus(): SecurityStatus {
        val privacyCompliance = privacyManager.getPrivacyComplianceReport()
        
        return SecurityStatus(
            isDatabaseEncrypted = getDatabaseKey() != null,
            hasSecureLockScreen = hasSecureLockScreen(),
            encryptedPreferencesAvailable = try {
                encryptedPreferences.getString("test", null)
                true
            } catch (e: Exception) {
                false
            },
            keystoreAvailable = try {
                keyStore.aliases().hasMoreElements()
                true
            } catch (e: Exception) {
                false
            },
            recommendations = getSecurityRecommendations(),
            privacyComplianceScore = privacyCompliance.complianceScore,
            hasValidConsent = privacyManager.consentState.value.hasValidConsent,
            dataProtectionEnabled = privacyManager.privacyState.value.settings.encryptSensitiveData
        )
    }
    
    /**
     * Perform comprehensive security audit
     */
    suspend fun performSecurityAudit(): SecurityAuditResult {
        val securityStatus = getSecurityStatus()
        val privacyCompliance = privacyManager.getPrivacyComplianceReport()
        val storageStats = secureDataStorage.getStorageStatistics()
        
        val issues = mutableListOf<SecurityIssue>()
        
        // Check encryption status
        if (!securityStatus.isDatabaseEncrypted) {
            issues.add(
                SecurityIssue(
                    type = SecurityIssueType.ENCRYPTION,
                    severity = SecuritySeverity.CRITICAL,
                    description = "Database encryption is not enabled",
                    recommendation = "Enable database encryption immediately"
                )
            )
        }
        
        // Check device security
        if (!securityStatus.hasSecureLockScreen) {
            issues.add(
                SecurityIssue(
                    type = SecurityIssueType.DEVICE_SECURITY,
                    severity = SecuritySeverity.HIGH,
                    description = "Device does not have secure lock screen",
                    recommendation = "Enable PIN, password, pattern, or biometric authentication"
                )
            )
        }
        
        // Check privacy compliance
        if (privacyCompliance.complianceScore < 80) {
            issues.add(
                SecurityIssue(
                    type = SecurityIssueType.PRIVACY_COMPLIANCE,
                    severity = SecuritySeverity.MEDIUM,
                    description = "Privacy compliance score is below recommended threshold",
                    recommendation = "Review and update privacy settings"
                )
            )
        }
        
        // Check consent status
        if (!privacyManager.consentState.value.hasValidConsent) {
            issues.add(
                SecurityIssue(
                    type = SecurityIssueType.CONSENT,
                    severity = SecuritySeverity.HIGH,
                    description = "User consent is missing or expired",
                    recommendation = "Request updated user consent"
                )
            )
        }
        
        return SecurityAuditResult(
            auditDate = System.currentTimeMillis(),
            overallScore = calculateOverallSecurityScore(issues),
            issues = issues,
            securityStatus = securityStatus,
            privacyCompliance = privacyCompliance,
            storageStatistics = storageStats,
            recommendations = generateSecurityRecommendations(issues)
        )
    }
    
    /**
     * Calculate overall security score
     */
    private fun calculateOverallSecurityScore(issues: List<SecurityIssue>): Int {
        var score = 100
        
        issues.forEach { issue ->
            val deduction = when (issue.severity) {
                SecuritySeverity.CRITICAL -> 30
                SecuritySeverity.HIGH -> 20
                SecuritySeverity.MEDIUM -> 10
                SecuritySeverity.LOW -> 5
            }
            score -= deduction
        }
        
        return score.coerceAtLeast(0)
    }
    
    /**
     * Generate security recommendations
     */
    private fun generateSecurityRecommendations(issues: List<SecurityIssue>): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (issues.any { it.type == SecurityIssueType.ENCRYPTION }) {
            recommendations.add("Enable end-to-end encryption for all sensitive data")
        }
        
        if (issues.any { it.type == SecurityIssueType.DEVICE_SECURITY }) {
            recommendations.add("Secure your device with strong authentication")
        }
        
        if (issues.any { it.type == SecurityIssueType.PRIVACY_COMPLIANCE }) {
            recommendations.add("Review and update privacy settings regularly")
        }
        
        if (issues.any { it.type == SecurityIssueType.CONSENT }) {
            recommendations.add("Ensure valid user consent for data processing")
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("Security posture is good. Continue monitoring.")
        }
        
        return recommendations
    }
    
    /**
     * Check if data processing is allowed for security purposes
     */
    fun isDataProcessingAllowed(purpose: DataProcessingPurpose): Boolean {
        return privacyManager.isDataProcessingAllowed(purpose)
    }
    
    /**
     * Encrypt sensitive data using the encryption service
     */
    suspend fun encryptSensitiveData(data: String): String {
        return secureDataStorage.encryptSensitiveField(data)
    }
    
    /**
     * Decrypt sensitive data using the encryption service
     */
    suspend fun decryptSensitiveData(encryptedData: String): String {
        return secureDataStorage.decryptSensitiveField(encryptedData)
    }
}

/**
 * Input validation types
 */
enum class InputType {
    TRANSACTION_AMOUNT,
    PRODUCT_NAME,
    CUSTOMER_ID,
    SPEAKER_ID,
    GENERAL_TEXT
}

/**
 * Validation result
 */
sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val reason: String) : ValidationResult()
}

/**
 * Security recommendation
 */
data class SecurityRecommendation(
    val type: SecurityRecommendationType,
    val severity: SecuritySeverity,
    val title: String,
    val description: String,
    val recommendation: String
)

/**
 * Security recommendation types
 */
enum class SecurityRecommendationType {
    DEVICE_SECURITY,
    ENCRYPTION,
    API_SECURITY,
    DATA_PROTECTION,
    NETWORK_SECURITY
}

/**
 * Security severity levels
 */
enum class SecuritySeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Security status
 */
data class SecurityStatus(
    val isDatabaseEncrypted: Boolean,
    val hasSecureLockScreen: Boolean,
    val encryptedPreferencesAvailable: Boolean,
    val keystoreAvailable: Boolean,
    val recommendations: List<SecurityRecommendation>,
    val privacyComplianceScore: Int = 0,
    val hasValidConsent: Boolean = false,
    val dataProtectionEnabled: Boolean = false
)

/**
 * Security audit result
 */
data class SecurityAuditResult(
    val auditDate: Long,
    val overallScore: Int,
    val issues: List<SecurityIssue>,
    val securityStatus: SecurityStatus,
    val privacyCompliance: PrivacyComplianceReport,
    val storageStatistics: StorageStatistics,
    val recommendations: List<String>
)

/**
 * Security issue
 */
data class SecurityIssue(
    val type: SecurityIssueType,
    val severity: SecuritySeverity,
    val description: String,
    val recommendation: String
)

/**
 * Security issue types
 */
enum class SecurityIssueType {
    ENCRYPTION,
    DEVICE_SECURITY,
    PRIVACY_COMPLIANCE,
    CONSENT,
    DATA_RETENTION,
    NETWORK_SECURITY
}