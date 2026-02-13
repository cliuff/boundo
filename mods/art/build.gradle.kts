plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "io.cliuff.boundo.art"
    compileSdk = 36

    defaultConfig {
        minSdk = 22
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin.jvmToolchain(17)
}

dependencies {
    implementation(libs.androidxCoreKtx)

    // override the version of transitive dependency in androidx.core, etc.
    implementation(libs.kotlinCoroutines)
    implementation(libs.kotlinStdlib)
    implementation(libs.google.smali)

    testImplementation(libs.junit4)
}