 
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val nexusKey = file("nexus.p12")

android {
    namespace = "com.nexus.cyberos"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.nexus.cyberos"
        minSdk = 29
        targetSdk = 34
        versionCode = 5
        versionName = "2.0"
    }
    if (nexusKey.exists()) {
        signingConfigs {
            create("nexus") {
                storeFile = nexusKey
                storePassword = "nexus2040"
                keyAlias = "nexus"
                keyPassword = "nexus2040"
                storeType = "pkcs12"
            }
        }
        buildTypes {
            getByName("debug") { signingConfig = signingConfigs.getByName("nexus") }
        }
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.core:core-ktx:1.13.1")
}
