plugins {
    id("asahi.kotlin-jvm")
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":core:model"))
    implementation(project(":core:network"))
    implementation("org.json:json:20240303")
}
