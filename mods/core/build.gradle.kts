plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "io.cliuff.boundo"
    compileSdk = 36

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin.jvmToolchain(17)
}

dependencies {
    implementation(libs.androidxCoreKtx)

    // override the version of transitive dependency in androidx.core, coil, etc.
    implementation(libs.kotlinCoroutines)
    implementation(libs.kotlinStdlib)
    implementation(libs.coil)
    implementation(libs.appIconLoader)

    testImplementation(libs.junit4)
}