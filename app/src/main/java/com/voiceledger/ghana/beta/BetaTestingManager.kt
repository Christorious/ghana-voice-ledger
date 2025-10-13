package com.voiceledger.ghana.beta

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BetaTestingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val analyticsService: AnalyticsService,
    private val feedbackRepository: FeedbackRepository
) {
    
    private val _betaStatus = MutableStateFlow(BetaStatus())
    val betaStatus: StateFlow<BetaStatus> = _betaStatus.asStateFlow()
    
    private val _betaFeatures = MutableStateFlow(getBetaFeatures())
    val betaFeatures: StateFlow<List<BetaFeature>> = _betaFeatures.asStateFlow()

    init {
        initializeBetaStatus()
    }

    private fun initializeBetaStatus() {
        val isBetaBuild = isBetaBuild()
        val isTestUser = isTestUser()
        
        _betaStatus.value = BetaStatus(
            isBetaBuild = isBetaBuild,
            isTestUser = isTestUser,
            betaVersion = getBetaVersion(),
            testGroup = getTestGroup(),
            enrollmentDate = getBetaEnrollmentDate(),
            feedbackCount = getFeedbackCount()
        )
        
        if (isBetaBuild) {
            trackBetaUsage()
        }
    }

    private fun isBetaBuild(): Boolean {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionName = packageInfo.versionName
            versionName?.contains("beta", ignoreCase = true) == true ||
            versionName?.contains("alpha", ignoreCase = true) == true ||
            versionName?.contains("rc", ignoreCase = true) == true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun isTestUser(): Boolean {
        // Check if user is enrolled in beta testing program
        val prefs = context.getSharedPreferences("beta_testing", Context.MODE_PRIVATE)
        return prefs.getBoolean("is_test_user", false)
    }

    private fun getBetaVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "Unknown"
        } catch (e: PackageManager.NameNotFoundException) {
            "Unknown"
        }
    }

    private fun getTestGroup(): TestGroup {
        val prefs = context.getSharedPreferences("beta_testing", Context.MODE_PRIVATE)
        val groupName = prefs.getString("test_group", TestGroup.CONTROL.name)
        return try {
            TestGroup.valueOf(groupName ?: TestGroup.CONTROL.name)
        } catch (e: IllegalArgumentException) {
            TestGroup.CONTROL
        }
    }

    private fun getBetaEnrollmentDate(): Long {
        val prefs = context.getSharedPreferences("beta_testing", Context.MODE_PRIVATE)
        return prefs.getLong("enrollment_date", 0L)
    }

    private fun getFeedbackCount(): Int {
        val prefs = context.getSharedPreferences("beta_testing", Context.MODE_PRIVATE)
        return prefs.getInt("feedback_count", 0)
    }

    fun enrollInBetaTesting(testGroup: TestGroup = TestGroup.BETA_GENERAL) {
        val prefs = context.getSharedPreferences("beta_testing", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("is_test_user", true)
            .putString("test_group", testGroup.name)
            .putLong("enrollment_date", System.currentTimeMillis())
            .apply()

        _betaStatus.value = _betaStatus.value.copy(
            isTestUser = true,
            testGroup = testGroup,
            enrollmentDate = System.currentTimeMillis()
        )

        analyticsService.trackEvent("beta_enrollment", mapOf(
            "test_group" to testGroup.name,
            "enrollment_date" to System.currentTimeMillis().toString()
        ))
    }

    fun leaveBetaTesting() {
        val prefs = context.getSharedPreferences("beta_testing", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("is_test_user", false)
            .remove("test_group")
            .remove("enrollment_date")
            .apply()

        _betaStatus.value = _betaStatus.value.copy(
            isTestUser = false,
            testGroup = TestGroup.CONTROL,
            enrollmentDate = 0L
        )

        analyticsService.trackEvent("beta_unenrollment", mapOf(
            "unenrollment_date" to System.currentTimeMillis().toString()
        ))
    }

    fun isBetaFeatureEnabled(featureId: String): Boolean {
        val currentStatus = _betaStatus.value
        if (!currentStatus.isBetaBuild && !currentStatus.isTestUser) {
            return false
        }

        val feature = _betaFeatures.value.find { it.id == featureId }
        return feature?.let { 
            it.isEnabled && it.testGroups.contains(currentStatus.testGroup)
        } ?: false
    }

    fun enableBetaFeature(featureId: String) {
        val updatedFeatures = _betaFeatures.value.map { feature ->
            if (feature.id == featureId) {
                feature.copy(isEnabled = true)
            } else {
                feature
            }
        }
        _betaFeatures.value = updatedFeatures

        analyticsService.trackEvent("beta_feature_enabled", mapOf(
            "feature_id" to featureId
        ))
    }

    fun disableBetaFeature(featureId: String) {
        val updatedFeatures = _betaFeatures.value.map { feature ->
            if (feature.id == featureId) {
                feature.copy(isEnabled = false)
            } else {
                feature
            }
        }
        _betaFeatures.value = updatedFeatures

        analyticsService.trackEvent("beta_feature_disabled", mapOf(
            "feature_id" to featureId
        ))
    }

    suspend fun submitBetaFeedback(feedback: BetaFeedback) {
        try {
            feedbackRepository.submitFeedback(
                Feedback(
                    type = FeedbackType.BUG_REPORT,
                    rating = feedback.rating,
                    category = feedback.category,
                    description = feedback.description,
                    email = feedback.email,
                    deviceInfo = feedback.deviceInfo,
                    timestamp = System.currentTimeMillis()
                )
            )

            // Update feedback count
            val prefs = context.getSharedPreferences("beta_testing", Context.MODE_PRIVATE)
            val currentCount = prefs.getInt("feedback_count", 0)
            prefs.edit().putInt("feedback_count", currentCount + 1).apply()

            _betaStatus.value = _betaStatus.value.copy(
                feedbackCount = currentCount + 1
            )

            analyticsService.trackEvent("beta_feedback_submitted", mapOf(
                "feedback_type" to feedback.type.name,
                "rating" to feedback.rating.toString()
            ))
        } catch (e: Exception) {
            analyticsService.trackEvent("beta_feedback_error", mapOf(
                "error" to e.message.orEmpty()
            ))
            throw e
        }
    }

    private fun trackBetaUsage() {
        analyticsService.trackEvent("beta_app_launch", mapOf(
            "beta_version" to getBetaVersion(),
            "test_group" to getTestGroup().name,
            "device_model" to Build.MODEL,
            "android_version" to Build.VERSION.RELEASE
        ))
    }

    fun getBetaInstructions(): List<BetaInstruction> {
        return listOf(
            BetaInstruction(
                id = "welcome",
                title = "Welcome to Beta Testing!",
                description = "Thank you for participating in the Ghana Voice Ledger beta program.",
                priority = BetaInstructionPriority.HIGH
            ),
            BetaInstruction(
                id = "feedback",
                title = "Provide Feedback",
                description = "Please report any bugs or issues you encounter using the feedback feature.",
                priority = BetaInstructionPriority.HIGH
            ),
            BetaInstruction(
                id = "test_features",
                title = "Test New Features",
                description = "Try out the latest features and let us know how they work for you.",
                priority = BetaInstructionPriority.MEDIUM
            ),
            BetaInstruction(
                id = "voice_training",
                title = "Voice Training",
                description = "Spend time training your voice profile for better recognition accuracy.",
                priority = BetaInstructionPriority.MEDIUM
            ),
            BetaInstruction(
                id = "offline_testing",
                title = "Test Offline Mode",
                description = "Try using the app without internet connection to test offline functionality.",
                priority = BetaInstructionPriority.LOW
            )
        )
    }

    private fun getBetaFeatures(): List<BetaFeature> {
        return listOf(
            BetaFeature(
                id = "enhanced_voice_recognition",
                name = "Enhanced Voice Recognition",
                description = "Improved accuracy for Ghanaian accents and local languages",
                isEnabled = true,
                testGroups = listOf(TestGroup.BETA_GENERAL, TestGroup.BETA_ADVANCED)
            ),
            BetaFeature(
                id = "advanced_analytics",
                name = "Advanced Analytics",
                description = "Detailed insights and transaction patterns",
                isEnabled = false,
                testGroups = listOf(TestGroup.BETA_ADVANCED)
            ),
            BetaFeature(
                id = "multi_currency_support",
                name = "Multi-Currency Support",
                description = "Support for multiple currencies beyond GHS",
                isEnabled = false,
                testGroups = listOf(TestGroup.BETA_GENERAL, TestGroup.BETA_ADVANCED)
            ),
            BetaFeature(
                id = "cloud_sync",
                name = "Cloud Synchronization",
                description = "Sync data across multiple devices",
                isEnabled = false,
                testGroups = listOf(TestGroup.BETA_ADVANCED)
            ),
            BetaFeature(
                id = "voice_commands",
                name = "Voice Commands",
                description = "Control the app using voice commands",
                isEnabled = true,
                testGroups = listOf(TestGroup.BETA_GENERAL, TestGroup.BETA_ADVANCED)
            )
        )
    }
}

data class BetaStatus(
    val isBetaBuild: Boolean = false,
    val isTestUser: Boolean = false,
    val betaVersion: String = "",
    val testGroup: TestGroup = TestGroup.CONTROL,
    val enrollmentDate: Long = 0L,
    val feedbackCount: Int = 0
)

data class BetaFeature(
    val id: String,
    val name: String,
    val description: String,
    val isEnabled: Boolean,
    val testGroups: List<TestGroup>
)

data class BetaInstruction(
    val id: String,
    val title: String,
    val description: String,
    val priority: BetaInstructionPriority
)

data class BetaFeedback(
    val type: BetaFeedbackType,
    val rating: Int,
    val category: FeedbackCategory,
    val description: String,
    val email: String?,
    val deviceInfo: DeviceInfo?
)

enum class TestGroup {
    CONTROL,
    BETA_GENERAL,
    BETA_ADVANCED
}

enum class BetaInstructionPriority {
    HIGH,
    MEDIUM,
    LOW
}

enum class BetaFeedbackType {
    BUG_REPORT,
    FEATURE_REQUEST,
    USABILITY_ISSUE,
    PERFORMANCE_ISSUE,
    GENERAL_FEEDBACK
}