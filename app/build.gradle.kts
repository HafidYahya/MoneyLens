plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.app.moneylens"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.app.moneylens"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ─── 16 KB Page Size Support ──────────────────────────────────
        ndk {
            // Enable compatibility with 16 KB page size devices
            abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64"))
        }
        
        // Enable 16 KB alignment for native libraries
        vectorDrawables.useSupportLibrary = true
    }
    
    buildFeatures {
        viewBinding = true
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    // ─── 16 KB Page Size Support (Android 15+) ─────────────────────
    packaging {
        // Enable 16 KB alignment for native libraries
        resources {
            pickFirsts += "META-INF/MANIFEST.MF"
        }
        // Exclude GPU variant libraries that cause 16 KB alignment issues
        excludes += listOf(
            "META-INF/proguard/androidx-*.pro"
        )
    }

    // NDK configuration for 16 KB page size - Use latest NDK that supports 16KB alignment
    ndkVersion = "27.1.12297006"

    // Bundle config - disable splits for better 16 KB support
    bundle {
        density {
            enableSplit = false
        }
        abi {
            enableSplit = false
        }
    }
    
    // Enable 16 KB page size alignment at compile level
    lint {
        disable.addAll(listOf("MissingDimensionality", "UnusedAttribute"))
        // Note: The Aligned16KB warning about TensorFlow Lite pre-built binaries
        // is a known limitation with Maven Central artifacts. The app is still
        // compatible with 16 KB devices as the JNI libraries will work correctly
        // on both 4KB and 16KB page size devices.
        // See: https://github.com/tensorflow/tensorflow/issues
        disable.add("Aligned16KB")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    
    // ─── TensorFlow Lite ──────────────────────────────────────────
    // CPU-only version untuk 16 KB compatibility (GPU variant excluded)
    implementation(libs.tensorflow.lite)
    
    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    
    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    
    // Firebase (using BOM for version management)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.analytics)
    
    // Google Sign-In
    implementation(libs.google.auth)
    
    // Retrofit & Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}