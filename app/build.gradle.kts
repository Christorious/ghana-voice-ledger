import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
    id("kotlin-parcelize")
    // Temporarily disabled for testing build without Firebase
    // id("com.google.gms.google-services")
    // id("com.google.firebase.crashlytics")
    // id("com.google.firebase.firebase-perf")
    // id("com.google.firebase.appdistribution")
}

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
        // Database encryption keys are generated at runtime via the Android Keystore
        buildConfigField("String", "APP_CENTER_SECRET", "\"${properties.getProperty("APP_CENTER_SECRET") ?: System.getenv("APP_CENTER_SECRET") ?: ""}\"")
        buildConfigField("boolean", "OFFLINE_MODE_ENABLED", "${properties.getProperty("OFFLINE_MODE_ENABLED") ?: System.getenv("OFFLINE_MODE_ENABLED") ?: "true"}")
        buildConfigField("boolean", "SPEAKER_IDENTIFICATION_ENABLED", "${properties.getProperty("SPEAKER_IDENTIFICATION_ENABLED") ?: System.getenv("SPEAKER_IDENTIFICATION_ENABLED") ?: "false"}")
        buildConfigField("boolean", "MULTI_LANGUAGE_ENABLED", "${properties.getProperty("MULTI_LANGUAGE_ENABLED") ?: System.getenv("MULTI_LANGUAGE_ENABLED") ?: "true"}")
        buildConfigField("boolean", "ENABLE_PERFORMANCE_MONITORING", "false")
        buildConfigField("boolean", "DEBUG_MODE", "true")
        buildConfigField("boolean", "LOGGING_ENABLED", "true")
        buildConfigField("boolean", "BETA_FEATURES_ENABLED", "false")
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
        }
        
        create("prod") {
            dimension = "environment"
            
            buildConfigField("String", "API_BASE_URL", "\"https://api.voiceledger.com/\"")
            buildConfigField("boolean", "OFFLINE_MODE_ENABLED", "true")
            buildConfigField("boolean", "SPEAKER_IDENTIFICATION_ENABLED", "false")
            buildConfigField("boolean", "MULTI_LANGUAGE_ENABLED", "true")
            buildConfigField("boolean", "BETA_FEATURES_ENABLED", "false")
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
        kotlinCompilerExtensionVersion = "1.5.8"
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

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.fragment:fragment-ktx:1.6.2")

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.foundation:foundation")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")

    // Hilt Dependency Injection
    implementation("com.google.dagger:hilt-android:2.48.1")
    kapt("com.google.dagger:hilt-compiler:2.48.1")
    implementation("androidx.hilt:hilt-work:1.1.0")
    kapt("androidx.hilt:hilt-compiler:1.1.0")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-paging:2.6.1")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Paging
    implementation("androidx.paging:paging-runtime-ktx:3.2.1")
    implementation("androidx.paging:paging-compose:3.2.1")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // JSON
    implementation("com.google.code.gson:gson:2.10.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Firebase - Temporarily disabled for testing build
    // implementation(platform("com.google.firebase:firebase-bom:32.7.1"))
    // implementation("com.google.firebase:firebase-analytics-ktx")
    // implementation("com.google.firebase:firebase-crashlytics-ktx")
    // implementation("com.google.firebase:firebase-perf-ktx")
    // implementation("com.google.firebase:firebase-messaging-ktx")
    // implementation("com.google.firebase:firebase-config-ktx")

    // App Center SDK
    val appCenterSdkVersion = "5.0.4"
    implementation("com.microsoft.appcenter:appcenter-analytics:$appCenterSdkVersion")
    implementation("com.microsoft.appcenter:appcenter-crashes:$appCenterSdkVersion")

    // Google Cloud Speech - Temporarily disabled for build
    // implementation("com.google.cloud:google-cloud-speech:4.21.0")
    // implementation("com.google.auth:google-auth-library-oauth2-http:1.19.0")

    // TensorFlow Lite
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-metadata:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0")

    // Audio Processing
    implementation("com.github.wendykierp:JTransforms:3.1")
    
    // WebRTC VAD (Voice Activity Detection) - Temporarily disabled for build
    // implementation("org.webrtc:google-webrtc:1.0.32006")

    // Security & Encryption
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.biometric:biometric:1.1.0")

    // Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")

    // Image Loading
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Logging
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Date/Time
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")

    // Charts (for analytics)
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Splash Screen
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("androidx.room:room-testing:2.6.1")
    testImplementation("com.google.truth:truth:1.1.4")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test.ext:junit:1.1.5")
    testImplementation("org.robolectric:robolectric:4.11.1")

    // Android Testing
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.navigation:navigation-testing:2.7.6")
    androidTestImplementation("androidx.work:work-testing:2.9.0")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.48.1")
    kaptAndroidTest("com.google.dagger:hilt-compiler:2.48.1")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.2.0")

    // Debug Tools
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.12")
}