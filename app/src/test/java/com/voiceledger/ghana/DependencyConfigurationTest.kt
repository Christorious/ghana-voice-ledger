package com.voiceledger.ghana

import org.junit.Test
import org.junit.Assert.*

/**
 * Test to verify that all required dependencies are available and classes can be loaded.
 * This test ensures that the dependency configuration changes don't break compilation.
 */
class DependencyConfigurationTest {

    @Test
    fun testFirebaseDependenciesAvailable() {
        // Test that Firebase classes are available
        try {
            Class.forName("com.google.firebase.analytics.FirebaseAnalytics")
            Class.forName("com.google.firebase.crashlytics.FirebaseCrashlytics")
            Class.forName("com.google.firebase.perf.FirebasePerformance")
            Class.forName("com.google.firebase.messaging.FirebaseMessaging")
            Class.forName("com.google.firebase.remoteconfig.FirebaseRemoteConfig")
        } catch (e: ClassNotFoundException) {
            fail("Firebase dependencies not available: ${e.message}")
        }
    }

    @Test
    fun testAppCenterDependenciesAvailable() {
        // Test that App Center classes are available
        try {
            Class.forName("com.microsoft.appcenter.AppCenter")
            Class.forName("com.microsoft.appcenter.analytics.Analytics")
            Class.forName("com.microsoft.appcenter.crashes.Crashes")
        } catch (e: ClassNotFoundException) {
            fail("App Center dependencies not available: ${e.message}")
        }
    }

    @Test
    fun testTensorFlowLiteDependenciesAvailable() {
        // Test that TensorFlow Lite classes are available
        try {
            Class.forName("org.tensorflow.lite.Interpreter")
            Class.forName("org.tensorflow.lite.gpu.CompatibilityList")
            Class.forName("org.tensorflow.lite.gpu.GpuDelegate")
            Class.forName("org.tensorflow.lite.support.tensorbuffer.TensorBuffer")
            Class.forName("org.tensorflow.lite.metadata.schema.ModelMetadata")
        } catch (e: ClassNotFoundException) {
            fail("TensorFlow Lite dependencies not available: ${e.message}")
        }
    }

    @Test
    fun testSQLCipherDependenciesAvailable() {
        // Test that SQLCipher classes are available
        try {
            Class.forName("net.sqlcipher.database.SQLiteDatabase")
            Class.forName("net.sqlcipher.database.SQLiteOpenHelper")
            Class.forName("net.sqlcipher.room.SupportFactory")
        } catch (e: ClassNotFoundException) {
            fail("SQLCipher dependencies not available: ${e.message}")
        }
    }

    @Test
    fun testSecurityDependenciesAvailable() {
        // Test that AndroidX Security classes are available
        try {
            Class.forName("androidx.security.crypto.EncryptedSharedPreferences")
            Class.forName("androidx.security.crypto.MasterKey")
            Class.forName("androidx.biometric.BiometricPrompt")
        } catch (e: ClassNotFoundException) {
            fail("AndroidX Security dependencies not available: ${e.message}")
        }
    }

    @Test
    fun testGoogleCloudSpeechDependenciesAvailable() {
        // Test that Google Cloud Speech classes are available
        try {
            Class.forName("com.google.cloud.speech.v1.SpeechClient")
            Class.forName("com.google.cloud.speech.v1.RecognitionConfig")
            Class.forName("com.google.auth.oauth2.GoogleCredentials")
        } catch (e: ClassNotFoundException) {
            fail("Google Cloud Speech dependencies not available: ${e.message}")
        }
    }

    @Test
    fun testWebRTCOptionalDependency() {
        // Test that WebRTC dependency is optional - should not fail if not available
        try {
            Class.forName("org.webrtc.VoiceDetection")
            // If class is available, that's fine too
        } catch (e: ClassNotFoundException) {
            // This is expected when WebRTC is disabled
            // The test passes because WebRTC is optional
        }
    }

    @Test
    fun testApplicationClassDependencies() {
        // Test that VoiceLedgerApplication can be loaded (tests all dependencies)
        try {
            Class.forName("com.voiceledger.ghana.VoiceLedgerApplication")
        } catch (e: ClassNotFoundException) {
            fail("VoiceLedgerApplication dependencies not available: ${e.message}")
        } catch (e: NoClassDefFoundError) {
            fail("VoiceLedgerApplication has missing dependencies: ${e.message}")
        }
    }

    @Test
    fun testSecurityManagerDependencies() {
        // Test that SecurityManager can be loaded (tests SQLCipher and Security dependencies)
        try {
            Class.forName("com.voiceledger.ghana.security.SecurityManager")
        } catch (e: ClassNotFoundException) {
            fail("SecurityManager dependencies not available: ${e.message}")
        } catch (e: NoClassDefFoundError) {
            fail("SecurityManager has missing dependencies: ${e.message}")
        }
    }

    @Test
    fun testTensorFlowSpeakerIdentifierDependencies() {
        // Test that TensorFlowLiteSpeakerIdentifier can be loaded
        try {
            Class.forName("com.voiceledger.ghana.ml.speaker.TensorFlowLiteSpeakerIdentifier")
        } catch (e: ClassNotFoundException) {
            fail("TensorFlowLiteSpeakerIdentifier dependencies not available: ${e.message}")
        } catch (e: NoClassDefFoundError) {
            fail("TensorFlowLiteSpeakerIdentifier has missing dependencies: ${e.message}")
        }
    }
}