buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // Override the R8 bundled with AGP 8.3.2 — it can't parse the Kotlin 2.2 metadata
        // emitted by litertlm-android's classes. This version does.
        classpath("com.android.tools:r8:8.11.32")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
