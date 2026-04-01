plugins {
    id("asahi.android-app")
}

import org.gradle.api.tasks.JavaExec

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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
}

tasks.register<JavaExec>("runPreview") {
    group = "verification"
    description = "Runs the Asahi debug preview flow on the JVM-ish app classpath"
    dependsOn("compileDebugKotlin")
    classpath = files(
        "$buildDir/tmp/kotlin-classes/debug",
        configurations.getByName("debugRuntimeClasspath")
    )
    mainClass.set("ai.shieldtv.app.debug.PreviewRunner")
}
