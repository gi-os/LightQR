plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Auto-versioning: CI passes VERSION_CODE / VERSION_NAME via env.
// Falls back to sensible defaults for local builds.
val vCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
val vName = System.getenv("VERSION_NAME") ?: "0.1.0-dev"

android {
    namespace = "com.gios.lightqr"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.gios.lightqr"
        minSdk = 29
        targetSdk = 34
        versionCode = vCode
        versionName = vName
    }

    // Consistent signing key so Obtainium can install updates in place.
    // Keystore is committed for a personal/hobby tool; override with env
    // (KEYSTORE_FILE / KEYSTORE_PASS / KEY_ALIAS / KEY_PASS) to use secrets.
    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_FILE") ?: "lightqr.keystore")
            storePassword = System.getenv("KEYSTORE_PASS") ?: "lightqr123"
            keyAlias = System.getenv("KEY_ALIAS") ?: "lightqr"
            keyPassword = System.getenv("KEY_PASS") ?: "lightqr123"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material3:material3")

    // CameraX — camera preview + frame analysis
    val camerax = "1.3.4"
    implementation("androidx.camera:camera-core:$camerax")
    implementation("androidx.camera:camera-camera2:$camerax")
    implementation("androidx.camera:camera-lifecycle:$camerax")
    implementation("androidx.camera:camera-view:$camerax")

    // ZXing core — pure-Java QR decoding (no Google Play Services needed).
    // This matters: LightOS ships without GMS, so ML Kit is not an option.
    implementation("com.google.zxing:core:3.5.3")
}
