import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.parcelize)
    // Temporarily disabled for testing build without Firebase
    // id("com.google.gms.google-services")
    // id("com.google.firebase.crashlytics")
    // id("com.google.firebase.firebase-perf")
    // id("com.google.firebase.appdistribution")
}

android {
    namespace = "com.voiceledger.ghana"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.voiceledger.ghana"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        buildConfigField("String", "ENCRYPTION_KEY", "\"${properties.getProperty("ENCRYPTION_KEY") ?: System.getenv("ENCRYPTION_KEY") ?: "12345678901234567890123456789012"}\"")
        buildConfigField("String", "DB_ENCRYPTION_KEY", "\"${properties.getProperty("DB_ENCRYPTION_KEY") ?: System.getenv("DB_ENCRYPTION_KEY") ?: "98765432109876543210987654321098"}\"")
        buildConfigField("String", "APP_CENTER_SECRET", "\"${properties.getProperty("APP_CENTER_SECRET") ?: System.getenv("APP_CENTER_SECRET") ?: ""}\"")
        buildConfigField("boolean", "OFFLINE_MODE_ENABLED", "${properties.getProperty("OFFLINE_MODE_ENABLED") ?: System.getenv("OFFLINE_MODE_ENABLED") ?: "true"}")
        buildConfigField("boolean", "SPEAKER_IDENTIFICATION_ENABLED", "${properties.getProperty("SPEAKER_IDENTIFICATION_ENABLED") ?: System.getenv("SPEAKER_IDENTIFICATION_ENABLED") ?: "false"}")
        buildConfigField("boolean", "MULTI_LANGUAGE_ENABLED", "${properties.getProperty("MULTI_LANGUAGE_ENABLED") ?: System.getenv("MULTI_LANGUAGE_ENABLED") ?: "true"}")
        buildConfigField("boolean", "ENABLE_PERFORMANCE_MONITORING", "false")
        buildConfigField("boolean", "DEBUG_MODE", "true")
        buildConfigField("boolean", "LOGGING_ENABLED", "true")
        buildConfigField("boolean", "BETA_FEATURES_ENABLED", "false")
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            
            buildConfigField("boolean", "DEBUG_MODE", "true")
            buildConfigField("boolean", "LOGGING_ENABLED", "true")
        }
        
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            buildConfigField("boolean", "DEBUG_MODE", "false")
            buildConfigField("boolean", "LOGGING_ENABLED", "false")
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
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment.ktx)

    // Compose BOM
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
    implementation(libs.gson)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // Firebase - Temporarily disabled for testing build
    // implementation(platform("com.google.firebase:firebase-bom:32.7.1"))
    // implementation("com.google.firebase:firebase-analytics-ktx")
    // implementation("com.google.firebase:firebase-crashlytics-ktx")
    // implementation("com.google.firebase:firebase-perf-ktx")
    // implementation("com.google.firebase:firebase-messaging-ktx")
    // implementation("com.google.firebase:firebase-config-ktx")

    // App Center SDK
    implementation(libs.appcenter.analytics)
    implementation(libs.appcenter.crashes)

    // Google Cloud Speech - Temporarily disabled for build
    // implementation("com.google.cloud:google-cloud-speech:4.21.0")
    // implementation("com.google.auth:google-auth-library-oauth2-http:1.19.0")

    // TensorFlow Lite
    implementation(libs.tensorflow.lite)
    implementation(libs.tensorflow.lite.support)
    implementation(libs.tensorflow.lite.metadata)
    implementation(libs.tensorflow.lite.gpu)

    // Audio Processing
    implementation(libs.jtransforms)
    
    // WebRTC VAD (Voice Activity Detection) - Temporarily disabled for build
    // implementation("org.webrtc:google-webrtc:1.0.32006")

    // Security & Encryption
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.biometric)

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
