plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.ksp)
    // implement parcelable interface by using annotation
    id("kotlin-parcelize")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

android {
    namespace = "io.cliuff.boundo.org"
    compileSdk = 35

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin.jvmToolchain(17)
}

dependencies {
    implementation(libs.androidxCoreKtx)
    implementation(libs.androidxRoomRuntime)
    implementation(libs.androidxRoom)
    ksp(libs.androidxRoomCompiler)

    testImplementation(libs.junit4)
}