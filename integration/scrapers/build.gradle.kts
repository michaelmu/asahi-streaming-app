plugins {
    id("asahi.kotlin-jvm")
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":core:model"))
    implementation(project(":core:network"))
    implementation(project(":integration:debrid-realdebrid"))
    implementation("org.json:json:20240303")
    implementation("org.jsoup:jsoup:1.18.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
}
