plugins {
    id("asahi.kotlin-jvm")
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":core:model"))
    implementation(project(":core:network"))
    implementation(project(":integration:debrid-realdebrid"))
    implementation("org.json:json:20240303")
}
