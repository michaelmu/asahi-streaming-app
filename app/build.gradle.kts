plugins {
    id("asahi.android-app")
}

import java.util.Properties
import org.gradle.api.tasks.JavaExec

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

fun localOrEnv(name: String): String? =
    (project.findProperty(name) as String?)
        ?: localProperties.getProperty(name)
        ?: System.getenv(name)

val releaseStoreFile = localOrEnv("ASAHI_RELEASE_STORE_FILE")
val releaseStorePassword = localOrEnv("ASAHI_RELEASE_STORE_PASSWORD")
val releaseKeyAlias = localOrEnv("ASAHI_RELEASE_KEY_ALIAS")
val releaseKeyPassword = localOrEnv("ASAHI_RELEASE_KEY_PASSWORD")
val hasReleaseSigning = !releaseStoreFile.isNullOrBlank() &&
    !releaseStorePassword.isNullOrBlank() &&
    !releaseKeyAlias.isNullOrBlank() &&
    !releaseKeyPassword.isNullOrBlank()

android {
    namespace = "ai.shieldtv.app"

    defaultConfig {
        applicationId = "ai.shieldtv.app"
        versionName = "0.1.0-asahi"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(requireNotNull(releaseStoreFile))
                storePassword = requireNotNull(releaseStorePassword)
                keyAlias = requireNotNull(releaseKeyAlias)
                keyPassword = requireNotNull(releaseKeyPassword)
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            isShrinkResources = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")

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
    implementation("androidx.activity:activity-ktx:1.9.2")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.media3:media3-common:1.4.1")
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("io.coil-kt:coil:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("com.google.zxing:core:3.5.3")
}

val debugKotlinClasses = layout.buildDirectory.dir("tmp/kotlin-classes/debug")

fun JavaExec.configureDebugRuntime(mainClassName: String) {
    dependsOn(":integration:metadata-tmdb:jar")
    dependsOn(":integration:debrid-realdebrid:jar")
    dependsOn(":integration:scrapers:jar")
    dependsOn(":integration:playback-media3:jar")
    dependsOn(":feature:search:jar")
    dependsOn(":feature:details:jar")
    dependsOn(":feature:sources:jar")
    dependsOn(":feature:player:jar")
    dependsOn(":feature:settings:jar")
    dependsOn(":domain:jar")
    dependsOn(":core:model:jar")
    dependsOn(":core:network:jar")
    dependsOn("compileDebugKotlin")
    classpath = files(
        debugKotlinClasses,
        configurations.getByName("debugRuntimeClasspath")
    )
    mainClass.set(mainClassName)
}

tasks.register<JavaExec>("runPreview") {
    group = "verification"
    description = "Runs the Asahi debug preview flow on the JVM-ish app classpath"
    configureDebugRuntime("ai.shieldtv.app.debug.PreviewRunner")
}

tasks.register<JavaExec>("startAuthPreview") {
    group = "verification"
    description = "Starts RD device auth and persists the active flow for later polling"
    configureDebugRuntime("ai.shieldtv.app.debug.StartAuthRunner")
}

tasks.register<JavaExec>("pollAuthPreview") {
    group = "verification"
    description = "Polls a previously started RD device auth flow from local debug storage"
    configureDebugRuntime("ai.shieldtv.app.debug.PollAuthRunner")
}

tasks.register<JavaExec>("pollAuthPreviewLong") {
    group = "verification"
    description = "Polls a previously started RD device auth flow repeatedly using the stored interval"
    configureDebugRuntime("ai.shieldtv.app.debug.PollAuthRunner")
    args("24")
}

tasks.register<JavaExec>("runAuthFlowPreview") {
    group = "verification"
    description = "Starts RD device auth and polls in the same live process"
    configureDebugRuntime("ai.shieldtv.app.debug.RunAuthFlowRunner")
}

tasks.register<JavaExec>("runTorrentioCacheProbe") {
    group = "verification"
    description = "Runs a direct Torrentio + Real-Debrid cache probe without the preview shell"
    configureDebugRuntime("ai.shieldtv.app.debug.RunTorrentioCacheProbe")
}

tasks.register<JavaExec>("runTorrentioRawProbe") {
    group = "verification"
    description = "Prints the raw debrid-aware Torrentio response for inspection"
    configureDebugRuntime("ai.shieldtv.app.debug.RunTorrentioRawProbe")
}

tasks.register<JavaExec>("runAuthPersistenceProbe") {
    group = "verification"
    description = "Polls the stored RD flow once and prints token persistence instrumentation"
    configureDebugRuntime("ai.shieldtv.app.debug.RunAuthPersistenceProbe")
}
