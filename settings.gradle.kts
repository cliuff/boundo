pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            // workaround for Google OSS Licenses plugin that does not support plugins DSL
            if (requested.id.id == "com.google.android.gms.oss-licenses-plugin") {
                useModule("com.google.android.gms:oss-licenses-plugin:${requested.version}")
            }
        }
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

include(":mods:org")
include(":app", ":api_viewing", ":wearable")
