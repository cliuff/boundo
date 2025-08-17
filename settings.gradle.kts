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
        // required by LibChecker-Rules-Bundle, SmoothCornerShape
        maven { url = uri("https://jitpack.io") }
    }
}

include(":mods:core")
include(":mods:org")
include(":app", ":api_viewing", ":wearable")
