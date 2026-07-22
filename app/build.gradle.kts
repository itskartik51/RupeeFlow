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
        kotlinCompilerExtensionVersion = "1.5.8" 
    }
}

dependencies {
    // Core Android features
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    
    // UI & Design (Mint/Lavender Theme)
    implementation("androidx.compose.ui:ui:1.6.0")
    implementation("androidx.compose.material3:material3:1.2.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    
    // Material Icons Extended
    implementation("androidx.compose.material:material-icons-extended:1.6.0")
    
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
