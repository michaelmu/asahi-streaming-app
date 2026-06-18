pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "shield-streaming-app"

include(
    ":app",
    ":core:model",
    ":core:common",
    ":core:network",
    ":core:data",
    ":domain",
    ":feature:search",
    ":feature:details",
    ":feature:sources",
    ":feature:player",
    ":feature:settings",
    ":integration:metadata-tmdb",
    ":integration:debrid-realdebrid",
    ":integration:scrapers",
    ":integration:playback-media3"
)
