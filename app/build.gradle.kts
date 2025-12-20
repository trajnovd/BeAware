plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.beaware.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.beaware.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    
    // UI
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)

    // Glass/blur UI
    implementation("com.github.Dimezis:BlurView:version-2.0.4")
    
    // MediaPipe Audio Classifier (YAMNet)
    implementation(libs.mediapipe.tasks.audio)
    
    // Google Play Services Location
    implementation(libs.play.services.location)
    
    // OpenStreetMap (OSMDroid) - API-key-free mapping
    implementation("org.osmdroid:osmdroid-android:6.1.18")
}

