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

    buildFeatures {
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
}

dependencies {
    // UI & Design (Mint/Lavender Theme aur Layout ke liye)
    implementation("androidx.compose.ui:ui:1.6.0")
    implementation("androidx.compose.material3:material3:1.2.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    
    // Google Sheets se data connect karne ke liye
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
