plugins {
    id("asahi.kotlin-jvm")
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
}
