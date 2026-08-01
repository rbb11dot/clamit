plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.clamit"
    // compileSdk 37 required by compose 1.12.0-beta01 (lint: ui-android dependency check).
    // targetSdk stays 35 — raising it would force edge-to-edge enforcement (separate task).
    compileSdk = 37

    defaultConfig {
        applicationId = "com.clamit"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        // Backend runs on the same device (Termux) — 127.0.0.1 is reachable there.
        // Override with -PapiBaseUrl=http://<host>:8080/ for a remote backend.
        val apiBaseUrl = (project.findProperty("apiBaseUrl") as String?) ?: "http://127.0.0.1:8080/"
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        abortOnError = false
    }
}

dependencies {
    // Material 3 Expressive (alpha25) requires compose 1.12.0-beta01; the BOM
    // (compose 1.11.4) is incompatible with it, so versions are pinned explicitly.
    implementation("androidx.compose.ui:ui:1.12.0-beta01")
    implementation("androidx.compose.material3:material3:1.5.0-alpha25")
    implementation("androidx.compose.ui:ui-tooling-preview:1.12.0-beta01")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    implementation("androidx.navigation:navigation-compose:2.9.0")

    // Koin
    implementation("io.insert-koin:koin-core:4.0.3")
    implementation("io.insert-koin:koin-android:4.0.3")
    implementation("io.insert-koin:koin-androidx-compose:4.0.3")

    // Retrofit + Gson
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Material icons extended
    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    debugImplementation("androidx.compose.ui:ui-tooling:1.12.0-beta01")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
