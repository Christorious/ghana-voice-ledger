import java.math.BigDecimal
import java.util.Properties
import java.io.FileInputStream
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
    id("jacoco")
    // Temporarily disabled for testing build without Firebase
    // id("com.google.gms.google-services")
    // id("com.google.firebase.crashlytics")
    // id("com.google.firebase.firebase-perf")
    // id("com.google.firebase.appdistribution")
}

// Apply Firebase plugins conditionally based on feature toggle
val firebaseEnabled = project.findProperty("feature.firebase.enabled")?.toString()?.toBoolean() ?: false
if (firebaseEnabled) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
    apply(plugin = "com.google.firebase.firebase-perf")
    apply(plugin = "com.google.firebase.appdistribution")
}

val googleCloudSpeechEnabled = project.findProperty("feature.googleCloudSpeech.enabled")?.toString()?.toBoolean() ?: false
val webRtcEnabled = project.findProperty("feature.webrtc.enabled")?.toString()?.toBoolean() ?: false

android {
    namespace = "com.voiceledger.ghana"
    compileSdk = 34

    // Load signing configuration
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val keystoreProperties = Properties()
    if (keystorePropertiesFile.exists()) {
        keystoreProperties.load(FileInputStream(keystorePropertiesFile))
    }

    signingConfigs {
        create("release") {
            if (keystoreProperties.containsKey("RELEASE_KEYSTORE_FILE")) {
                storeFile = file(keystoreProperties["RELEASE_KEYSTORE_FILE"] as String)
                storePassword = keystoreProperties["RELEASE_KEYSTORE_PASSWORD"] as String
                keyAlias = keystoreProperties["RELEASE_KEY_ALIAS"] as String
                keyPassword = keystoreProperties["RELEASE_KEY_PASSWORD"] as String
                
                // Enable v2 and v3 signing for better security and compatibility
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    defaultConfig {
        applicationId = "com.voiceledger.ghana"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "com.voiceledger.ghana.HiltTestRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Room schema export
        kapt {
            arguments {
                arg("room.schemaLocation", "$projectDir/schemas")
            }
        }

        // Build config fields from local.properties or environment variables
        val properties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            properties.load(localPropertiesFile.inputStream())
        }

        buildConfigField("String", "GOOGLE_CLOUD_API_KEY", "\"${properties.getProperty("GOOGLE_CLOUD_API_KEY") ?: System.getenv("GOOGLE_CLOUD_API_KEY") ?: ""}\"")
        buildConfigField("String", "FIREBASE_PROJECT_ID", "\"${properties.getProperty("FIREBASE_PROJECT_ID") ?: System.getenv("FIREBASE_PROJECT_ID") ?: "ghana-voice-ledger"}\"")
        // Database encryption keys are generated at runtime via the Android Keystore
        buildConfigField("String", "APP_CENTER_SECRET", "\"${properties.getProperty("APP_CENTER_SECRET") ?: System.getenv("APP_CENTER_SECRET") ?: ""}\"")
        buildConfigField("boolean", "OFFLINE_MODE_ENABLED", "${properties.getProperty("OFFLINE_MODE_ENABLED") ?: System.getenv("OFFLINE_MODE_ENABLED") ?: "true"}")
        buildConfigField("boolean", "SPEAKER_IDENTIFICATION_ENABLED", "${properties.getProperty("SPEAKER_IDENTIFICATION_ENABLED") ?: System.getenv("SPEAKER_IDENTIFICATION_ENABLED") ?: "false"}")
        buildConfigField("boolean", "MULTI_LANGUAGE_ENABLED", "${properties.getProperty("MULTI_LANGUAGE_ENABLED") ?: System.getenv("MULTI_LANGUAGE_ENABLED") ?: "true"}")
        buildConfigField("boolean", "ENABLE_PERFORMANCE_MONITORING", "false")
        buildConfigField("boolean", "DEBUG_MODE", "true")
        buildConfigField("boolean", "LOGGING_ENABLED", "true")
        buildConfigField("boolean", "BETA_FEATURES_ENABLED", "false")
        buildConfigField("boolean", "FEATURE_FIREBASE_ENABLED", "$firebaseEnabled")
        buildConfigField("boolean", "FEATURE_GOOGLE_CLOUD_SPEECH_ENABLED", "$googleCloudSpeechEnabled")
        buildConfigField("boolean", "FEATURE_WEBRTC_ENABLED", "$webRtcEnabled")
        
        // Feature toggles for external services
        buildConfigField("boolean", "FIREBASE_ENABLED", "${properties.getProperty("FIREBASE_ENABLED") ?: System.getenv("FIREBASE_ENABLED") ?: "false"}")
        buildConfigField("boolean", "GOOGLE_CLOUD_SPEECH_ENABLED", "${properties.getProperty("GOOGLE_CLOUD_SPEECH_ENABLED") ?: System.getenv("GOOGLE_CLOUD_SPEECH_ENABLED") ?: "false"}")
        buildConfigField("boolean", "WEBRTC_ENABLED", "${properties.getProperty("WEBRTC_ENABLED") ?: System.getenv("WEBRTC_ENABLED") ?: "false"}")
    }

    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            
            buildConfigField("String", "API_BASE_URL", "\"https://api-dev.voiceledger.com/\"")
            buildConfigField("boolean", "OFFLINE_MODE_ENABLED", "false")
            buildConfigField("boolean", "SPEAKER_IDENTIFICATION_ENABLED", "true")
            buildConfigField("boolean", "MULTI_LANGUAGE_ENABLED", "true")
            buildConfigField("boolean", "BETA_FEATURES_ENABLED", "true")
            
            // Enable all features in dev
            buildConfigField("boolean", "FIREBASE_ENABLED", "true")
            buildConfigField("boolean", "GOOGLE_CLOUD_SPEECH_ENABLED", "true")
            buildConfigField("boolean", "WEBRTC_ENABLED", "true")
        }
        
        create("staging") {
            dimension = "environment"
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            
            buildConfigField("String", "API_BASE_URL", "\"https://api-staging.voiceledger.com/\"")
            buildConfigField("boolean", "OFFLINE_MODE_ENABLED", "true")
            buildConfigField("boolean", "SPEAKER_IDENTIFICATION_ENABLED", "true")
            buildConfigField("boolean", "MULTI_LANGUAGE_ENABLED", "true")
            buildConfigField("boolean", "BETA_FEATURES_ENABLED", "false")
            
            // Enable selective features in staging
            buildConfigField("boolean", "FIREBASE_ENABLED", "true")
            buildConfigField("boolean", "GOOGLE_CLOUD_SPEECH_ENABLED", "false")
            buildConfigField("boolean", "WEBRTC_ENABLED", "false")
        }
        
        create("prod") {
            dimension = "environment"
            
            buildConfigField("String", "API_BASE_URL", "\"https://api.voiceledger.com/\"")
            buildConfigField("boolean", "OFFLINE_MODE_ENABLED", "true")
            buildConfigField("boolean", "SPEAKER_IDENTIFICATION_ENABLED", "false")
            buildConfigField("boolean", "MULTI_LANGUAGE_ENABLED", "true")
            buildConfigField("boolean", "BETA_FEATURES_ENABLED", "false")
            
            // Minimal features in production
            buildConfigField("boolean", "FIREBASE_ENABLED", "false")
            buildConfigField("boolean", "GOOGLE_CLOUD_SPEECH_ENABLED", "false")
            buildConfigField("boolean", "WEBRTC_ENABLED", "false")
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            
            buildConfigField("boolean", "DEBUG_MODE", "true")
            buildConfigField("boolean", "LOGGING_ENABLED", "true")
            
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
        
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            isJniDebuggable = false
            isRenderscriptDebuggable = false
            
            // Apply signing configuration if available
            signingConfig = signingConfigs.findByName("release")
            
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            buildConfigField("boolean", "DEBUG_MODE", "false")
            buildConfigField("boolean", "LOGGING_ENABLED", "false")
            buildConfigField("boolean", "ENABLE_PERFORMANCE_MONITORING", "false")
            
            // Add version information for release builds
            buildConfigField("String", "BUILD_TYPE", "\"release\"")
            buildConfigField("long", "BUILD_TIME", "${System.currentTimeMillis()}L")
        }
        
        create("beta") {
            initWith(getByName("release"))
            isDebuggable = true
            applicationIdSuffix = ".beta"
            versionNameSuffix = "-beta"
            
            buildConfigField("boolean", "BETA_FEATURES_ENABLED", "true")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    
    // Create alias tasks to avoid ambiguity in automated checks
    task("checkDebugAarMetadata") {
        dependsOn("checkDevDebugAarMetadata", "checkStagingDebugAarMetadata", "checkProdDebugAarMetadata")
        description = "Alias for checking debug AAR metadata for all variants"
    }
    
    task("checkReleaseAarMetadata") {
        dependsOn("checkDevReleaseAarMetadata", "checkStagingReleaseAarMetadata", "checkProdReleaseAarMetadata")
        description = "Alias for checking release AAR metadata for all variants"
    }
}

jacoco {
    toolVersion = "0.8.11"
}

val coverageExclusions = listOf(
    "**/R.class",
    "**/R$*.class",
    "**/BuildConfig.*",
    "**/Manifest*.*",
    "**/*Test*.*",
    "**/*_Factory.*",
    "**/*_MembersInjector.*",
    "**/*_GeneratedInjector.*",
    "**/*Dagger*.*",
    "**/*Hilt*.*",
    "**/hilt_aggregated_deps/**",
    "**/dagger/hilt/internal/**"
)

val coverageSourceDirs = files("src/main/java", "src/main/kotlin")

val coverageClassDirs = files(
    fileTree("${buildDir}/tmp/kotlin-classes/debug") {
        exclude(coverageExclusions)
    },
    fileTree("${buildDir}/intermediates/javac/debug/classes") {
        exclude(coverageExclusions)
    }
)

val coverageExecutionData = fileTree(buildDir) {
    include("**/*.exec", "**/*.ec")
}

tasks.register<JacocoReport>("jacocoTestReport") {
    group = "verification"
    description = "Generates JaCoCo coverage reports for the debug build."

    dependsOn("testDebugUnitTest")
    classDirectories.setFrom(coverageClassDirs)
    sourceDirectories.setFrom(coverageSourceDirs)
    executionData.setFrom(coverageExecutionData)

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
}

tasks.register<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    group = "verification"
    description = "Verifies that code coverage meets the minimum threshold of 70%."

    dependsOn("jacocoTestReport")
    classDirectories.setFrom(coverageClassDirs)
    sourceDirectories.setFrom(coverageSourceDirs)
    executionData.setFrom(coverageExecutionData)

    violationRules {
        rule {
            limit {
                minimum = BigDecimal("0.70")
            }
        }
    }
}

tasks.named("check") {
    dependsOn("jacocoTestCoverageVerification")
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment.ktx)

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.foundation:foundation")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.foundation)

    // Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // Hilt Dependency Injection
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.androidx.hilt.work)
    kapt(libs.androidx.hilt.compiler)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)
    implementation(libs.androidx.room.paging)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Paging
    implementation(libs.androidx.paging.runtime.ktx)
    implementation(libs.androidx.paging.compose)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    // JSON
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation(libs.gson)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // Firebase - Always required for analytics services used throughout the app
    // Firebase plugins and services can be disabled via build flags, but dependencies remain
    implementation(platform("com.google.firebase:firebase-bom:32.7.1"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-perf-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-config-ktx")

    // App Center SDK - Always required for analytics and crash reporting
    implementation(libs.appcenter.analytics)
    implementation(libs.appcenter.crashes)

    // Google Cloud Speech - Optional, can be disabled via runtime flags
    // Using Google ML Kit for on-device speech recognition instead
    // For cloud-based speech-to-text, use REST API with OkHttp
    // implementation("com.google.cloud:google-cloud-speech:4.21.0")
    // implementation("com.google.auth:google-auth-library-oauth2-http:1.19.0")

    // TensorFlow Lite - Always required for speaker identification and ML models
    implementation(libs.tensorflow.lite)
    implementation(libs.tensorflow.lite.support)
    implementation(libs.tensorflow.lite.metadata)
    implementation(libs.tensorflow.lite.gpu)

    // Audio Processing
    implementation(libs.jtransforms)
    
    // WebRTC VAD (Voice Activity Detection) - Feature toggled via build flags
    if (webRtcEnabled && project.hasProperty("WEBRTC_ENABLED") && project.property("WEBRTC_ENABLED") == "true") {
        implementation("org.webrtc:google-webrtc:1.0.32006")
    }

    // Security & Encryption - Always required for database encryption and secure storage
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.biometric)
    implementation("net.zetetic:android-database-sqlcipher:4.5.4")

    // Permissions
    implementation(libs.accompanist.permissions)

    // Image Loading
    implementation(libs.coil.compose)

    // Logging
    implementation(libs.timber)

    // Date/Time
    implementation(libs.kotlinx.datetime)

    // Charts (for analytics)
    implementation(libs.mpandroidchart)

    // Splash Screen
    implementation(libs.androidx.core.splashscreen)


    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.arch.core.testing)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.robolectric)

    // Android Testing
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.navigation.testing)
    androidTestImplementation(libs.androidx.work.testing)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.hilt.android.testing)
    kaptAndroidTest(libs.hilt.compiler)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.uiautomator)

    // Debug Tools
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.leakcanary)
}
