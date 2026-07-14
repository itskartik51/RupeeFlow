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

    // YEH NAYI SETTING ADD KI HAI (Dono ko Version 17 par lane ke liye)
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
    
    // Nayi Library: Material Icons Extended (Eye aur Graph icons ke liye)
    implementation("androidx.compose.material:material-icons-extended:1.6.0")
    
    // Google Sheets se connect karne ke liye
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Nayi Library: Coil (Bank ke HD Logos fetch aur render karne ke liye)
    implementation("io.coil-kt:coil-compose:2.4.0")
}
