plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // NEW: Firebase Google Services Plugin
    id("com.google.gms.google-services") version "4.4.1"
}

android {
    namespace = "com.kartikey.rupeeflow"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.kartikey.rupeeflow"
        minSdk = 24
        targetSdk = 34
        versionCode = 8
        versionName = "1.00.008"
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
            // OPTIMIZATION: Unused code aur Faltu XML resources ko hatayega
            isMinifyEnabled = true
            isShrinkResources = true
            
            // BYPASS: Fake/Corrupted PNG errors ko ignore karne ke liye false rakha hai
            isCrunchPngs = false 
            
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
        kotlinCompilerExtensionVersion = "1.5.10" 
    }
}

dependencies {
    // Core Android features
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    
    // Biometric Security Lock Library (Fix for BiometricPrompt)
    implementation("androidx.biometric:biometric:1.1.0")

    // UI & Design (Mint/Lavender Theme) 
    implementation("androidx.compose.ui:ui:1.7.0")
    implementation("androidx.compose.animation:animation:1.7.0") 
    implementation("androidx.compose.material3:material3:1.3.0")
    implementation("androidx.activity:activity-compose:1.9.0")
    
    // Material Icons Extended 
    implementation("androidx.compose.material:material-icons-extended:1.7.0")
    
    // Google Sheets API & Images (Ab OkHttp hatane ka time aayega next step me)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.coil-kt:coil-compose:2.4.0")

    // ==========================================
    // NEW LIBRARIES: FIREBASE SDK
    // ==========================================
    // Firebase BoM (Bill of Materials) - Versions manage karega
    implementation(platform("com.google.firebase:firebase-bom:32.7.2"))
    // Firestore Database Dependency
    implementation("com.google.firebase:firebase-firestore")

    // ==========================================
    // NEW LIBRARIES: QR & SCANNER ENGINE
    // ==========================================
    
    // 1. ZXing 
    implementation("com.google.zxing:core:3.5.3")
    
    // 2. CameraX 
    val cameraxVersion = "1.3.2"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    
    // 3. Google ML Kit 
    implementation("com.google.android.gms:play-services-mlkit-barcode-scanning:18.3.0")
}
