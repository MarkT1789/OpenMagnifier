import java.util.Properties
import java.io.FileInputStream

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "io.uglydog.magnifier"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    signingConfigs {
        create("release") {
            keyAlias = keystoreProperties.getProperty("keyAlias") ?: "missing_alias"
            keyPassword = keystoreProperties.getProperty("keyPassword") ?: "missing_password"
            storePassword = keystoreProperties.getProperty("storePassword") ?: "missing_password"
            val path = keystoreProperties.getProperty("storeFile")
            if (path != null) {
                storeFile = file(path)
            }
        }
    }

    defaultConfig {
        applicationId = "io.uglydog.magnifier"
        minSdk = 24
        targetSdk = 36
        versionCode = 108000
        versionName = "1.8.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    val cameraXVersion = "1.6.1"

    // Core CameraX libraries
    implementation("androidx.camera:camera-core:$cameraXVersion")
    implementation("androidx.camera:camera-camera2:$cameraXVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraXVersion")
    implementation("androidx.camera:camera-view:$cameraXVersion")

    // Night Mode Extension
    implementation("androidx.camera:camera-extensions:$cameraXVersion")

    // UI Components
    implementation("androidx.cardview:cardview:1.0.0") // Needed for the image thumbnail

    // Preferences
    implementation("androidx.preference:preference-ktx:1.2.1")

    // ImageView replacement
    implementation("com.davemorrissey.labs:subsampling-scale-image-view-androidx:3.10.0")

    // Text Recognition (bundled)
    // Latin (English, Spanish, French, etc.)
    implementation("com.google.mlkit:text-recognition:16.0.1")
    // Chinese
    implementation("com.google.mlkit:text-recognition-chinese:16.0.1")
    // Devanagari
    implementation("com.google.mlkit:text-recognition-devanagari:16.0.1")
    // Japanese
    implementation("com.google.mlkit:text-recognition-japanese:16.0.1")

    // Google ML Kit Text Translation dependency
    implementation("com.google.mlkit:translate:17.0.3")
}
