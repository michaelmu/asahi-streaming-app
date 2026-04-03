plugins {
    id("asahi.android-library")
}

android {
    namespace = "ai.shieldtv.app.integration.playback.media3"
}

dependencies {
    testImplementation("junit:junit:4.13.2")

    implementation(project(":domain"))
    implementation(project(":core:model"))
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-common:1.4.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
}
