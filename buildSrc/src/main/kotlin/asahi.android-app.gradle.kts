import java.util.Properties
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.application")
    kotlin("android")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

android {
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        val tmdbApiKey = (project.findProperty("TMDB_API_KEY") as String?)
            ?: localProperties.getProperty("TMDB_API_KEY")
            ?: System.getenv("TMDB_API_KEY")
            ?: ""
        val gitSha = runCatching {
            providers.exec {
                commandLine("git", "rev-parse", "--short", "HEAD")
            }.standardOutput.asText.get().trim()
        }.getOrDefault("unknown")
        buildConfigField("String", "TMDB_API_KEY", "\"$tmdbApiKey\"")
        buildConfigField("boolean", "TMDB_KEY_EMBEDDED", if (tmdbApiKey.isNotBlank()) "true" else "false")
        buildConfigField("String", "GIT_SHA", "\"$gitSha\"")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
}

repositories {
    google()
    mavenCentral()
}
