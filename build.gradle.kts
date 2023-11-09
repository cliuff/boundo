plugins {
    alias(libs.plugins.android) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.google.gms.licenses) apply false
}

tasks{
    val clean by registering(Delete::class){
        delete(rootProject.buildDir)
    }
}
