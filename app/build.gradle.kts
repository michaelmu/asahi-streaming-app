plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "ai.shieldtv.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "ai.shieldtv.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0-asahi"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":core:model"))
    implementation(project(":feature:search"))
    implementation(project(":feature:details"))
    implementation(project(":feature:sources"))
    implementation(project(":feature:player"))
    implementation(project(":feature:settings"))
}
