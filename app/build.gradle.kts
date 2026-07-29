plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.kartikey.rupeeflow"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.kartikey.rupeeflow"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        getByName("debug") {
            val keystoreFile = file("../debug.keystore")
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
        create("release") {
            val keystoreFile = file("../release.keystore")
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = "rupeeflow2026"
                keyAlias = "rupeeflow"
                keyPassword = "rupeeflow2026"
            }
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }
        getByName("release") {
            isMinifyEnabled = false
            // Ye naya code hai jo strict PNG checking ko band karega
            isCrunchPngs = false 
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
    
    composeOptions {
        // Updated to support Kotlin 1.9.22 perfectly
        kotlinCompilerExtensionVersion = "1.5.10" 
    }
}

dependencies {
    // Core Android features
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    
    // UI & Design (Mint/Lavender Theme) - UPDATED TO 1.7.0 FOR APPLE ANIMATIONS
    implementation("androidx.compose.ui:ui:1.7.0")
    implementation("androidx.compose.animation:animation:1.7.0") // Animation library explicitly added
    implementation("androidx.compose.material3:material3:1.3.0")
    implementation("androidx.activity:activity-compose:1.9.0")
    
    // Material Icons Extended - UPDATED TO MATCH UI
    implementation("androidx.compose.material:material-icons-extended:1.7.0")
    
    // Google Sheets API & Images
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.coil-kt:coil-compose:2.4.0")

    // ==========================================
    // NEW LIBRARIES: QR & SCANNER ENGINE
    // ==========================================
    
    // 1. ZXing (For Premium Rounded QR Generation)
    implementation("com.google.zxing:core:3.5.3")
    
    // 2. CameraX (For Smooth 60FPS Camera Feed)
    val cameraxVersion = "1.3.2"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    
    // 3. Google ML Kit (UNBUNDLED - 0 MB Size Increase)
    implementation("com.google.android.gms:play-services-mlkit-barcode-scanning:18.3.0")
}
