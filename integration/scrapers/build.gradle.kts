plugins {
    id("asahi.kotlin-jvm")
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":core:model"))
}
