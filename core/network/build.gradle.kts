plugins {
    id("asahi.kotlin-jvm")
}

dependencies {
    implementation(project(":core:common"))
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
}
