// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.hilt.android) apply false
    // Temporarily disabled for testing build without Firebase
    // id("com.google.gms.google-services") version "4.4.0" apply false
    // id("com.google.firebase.crashlytics") version "2.9.9" apply false
    // id("com.google.firebase.firebase-perf") version "1.4.2" apply false
    // id("com.google.firebase.appdistribution") version "4.0.1" apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}