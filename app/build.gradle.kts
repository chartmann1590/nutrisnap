import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val localProps = Properties()
val localPropsFile = rootProject.file("local.properties")
if (localPropsFile.exists()) {
    localProps.load(FileInputStream(localPropsFile))
}

android {
    namespace = "com.charles.nutrisnap"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.charles.nutrisnap"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "0.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // Default Gemma 4 model (E2B) — overridable. See ai/ + download/.
        buildConfigField(
            "String",
            "MODEL_URL",
            "\"https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm\""
        )
        buildConfigField("String", "MODEL_FILE_NAME", "\"gemma-4-E2B-it.litertlm\"")
        buildConfigField("long", "MODEL_SIZE_BYTES", "2588147712L") // ~2.4 GiB (E2B)
        buildConfigField("String", "MODEL_URL_E4B", "\"https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it.litertlm\"")
        buildConfigField("String", "MODEL_FILE_NAME_E4B", "\"gemma-4-E4B-it.litertlm\"")
        buildConfigField("long", "MODEL_SIZE_BYTES_E4B", "3659530240L") // ~3.4 GiB (E4B)
    }

    signingConfigs {
        create("release") {
            val sf = localProps.getProperty("nutrisnap.storeFile")
            if (sf != null) storeFile = rootProject.file(sf)
            storePassword = localProps.getProperty("nutrisnap.storePassword") ?: ""
            keyAlias = localProps.getProperty("nutrisnap.keyAlias") ?: ""
            keyPassword = localProps.getProperty("nutrisnap.keyPassword") ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf("-Xskip-metadata-version-check")
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            // Page-align & keep native libs uncompressed so they map directly (required for 16 KB
            // page devices and faster load). ELF segment alignment of prebuilt libs is upstream.
            useLegacyPackaging = false
        }
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true // Robolectric needs merged resources/manifest
            isReturnDefaultValues = true
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

hilt {
    // Run all Dagger processing through KSP (not the separate aggregating javac task) so the
    // kotlin-metadata-jvm override on the ksp classpath governs metadata parsing.
    enableAggregatingTask = false
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.ui.tooling)

    // DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    // Dagger 2.57+ unshades kotlin-metadata-jvm; override it so the annotation processor
    // can read Kotlin 2.2 metadata emitted by litertlm-android's classes.
    ksp(libs.kotlin.metadata.jvm)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // Data
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)
    implementation(libs.work.runtime.ktx)

    // Media / camera / barcode
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.coil.compose)
    implementation(libs.mlkit.barcode)

    // Networking / serialization / coroutines
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    // Phase 4 — LiteRT-LM (Gemma 4 on-device). Maven artifact: the runtime version must
    // be new enough to load the current Gemma 4 .litertlm (multi-signature vision encoder).
    implementation(libs.litertlm)
    implementation(libs.gson)

    // Tests
    testImplementation(libs.junit)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.room.testing)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
}
