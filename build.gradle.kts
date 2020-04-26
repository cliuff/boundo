plugins {
//    "kotlin.dsl"
//    id("org.jetbrains.kotlin.kapt") version "1.3.20"
//    id("org.jetbrains.kotlin.android") version "1.3.20"
//    id("org.jetbrains.dokka-android") version "0.9.17"
}

buildscript {
    repositories {
        google()
        jcenter()
    }
    val Versions = object {
        val kotlin = "1.3.72"
        val dokka = "0.9.18"
        val androidxNavigation = "2.2.2"
        val googlePlayServicesOSSLicensesPlugin = "0.10.0"
        val androidGradlePlugin = "3.6.0"
    }
    dependencies {
        classpath("com.android.tools.build:gradle:${Versions.androidGradlePlugin}")
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:${Versions.androidxNavigation}")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}")
        classpath("org.jetbrains.kotlin:kotlin-android-extensions:${Versions.kotlin}")
        classpath("org.jetbrains.dokka:dokka-android-gradle-plugin:${Versions.dokka}")
        classpath("com.google.android.gms:oss-licenses-plugin:${Versions.googlePlayServicesOSSLicensesPlugin}")
    }
}

allprojects{
    repositories {
        google()
        jcenter()
    }
}

tasks{
    val clean by registering(Delete::class){
        delete(rootProject.buildDir)
    }
}

