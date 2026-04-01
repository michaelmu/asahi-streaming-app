plugins {
    id("asahi.android-app")
}

android {
    namespace = "ai.shieldtv.app"

    defaultConfig {
        applicationId = "ai.shieldtv.app"
        versionName = "0.1.0-asahi"
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
    implementation(project(":integration:metadata-tmdb"))
    implementation(project(":integration:debrid-realdebrid"))
    implementation(project(":integration:scrapers"))
    implementation(project(":integration:playback-media3"))
}
