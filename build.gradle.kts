plugins {
    alias(libs.plugins.android) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.dynamic.feature) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.aboutLibraries) apply false
}

tasks{
    val clean by registering(Delete::class){
        delete(rootProject.buildDir)
    }
}
