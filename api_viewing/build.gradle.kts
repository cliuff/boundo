plugins {
    id("com.android.dynamic-feature")
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.ksp)
    // implement parcelable interface by using annotation
    id("kotlin-parcelize")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    sourceSets {
        getByName("main").java.srcDir("src/main/kotlin")
    }
    // namespace is used by R and BuildConfig classes
    namespace = "com.madness.collision.unit.api_viewing"
    compileSdk = 34

    flavorDimensions.add("arch")
    productFlavors {
        create("full") {
            dimension = "arch"
        }
        create("arm") {
            dimension = "arch"
        }
        create("x86") {
            dimension = "arch"
        }
    }

    defaultConfig {
        minSdk = 23
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        getByName("debug") {
            isJniDebuggable = false
            isRenderscriptDebuggable = false
            renderscriptOptimLevel = 3
        }
        getByName("release") {
            isJniDebuggable = false
            isRenderscriptDebuggable = false
            renderscriptOptimLevel = 3
        }
    }

    compileOptions {
        // Flag to enable support for the new Java 8+ APIs
        isCoreLibraryDesugaringEnabled = true
        targetCompatibility = JavaVersion.VERSION_17
        sourceCompatibility = JavaVersion.VERSION_17
    }
    kotlin.jvmToolchain(17)
    buildFeatures.viewBinding = true
    buildFeatures.compose = true
    composeOptions {
        // Jetpack Compose compiler version
        kotlinCompilerExtensionVersion = libs.versions.androidxComposeCompiler.get()
    }
}

dependencies {
    coreLibraryDesugaring(libs.androidDesugaring)
    listOf(
        fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))),
        project(":app"),
        libs.androidxDocumentFile,
        libs.androidxSwipeRefreshLayout,
        libs.androidxRecyclerView,
        libs.androidxPreference,
        libs.androidxRoom,
        libs.androidxRoomRuntime,
        libs.mpChart,
        libs.openCsv,
        libs.androidDeviceNames,
        libs.smoothCornerCompose,
        libs.androidxConstraintLayoutCompose,
        libs.libCheckerRules,
        libs.googleAccompanistFlowLayout,
        project(":apk-parser"),
    ).forEach { implementation(it) }
    implementation(libs.bundles.dynamicFeatureBasics)

    listOf(libs.mockito, libs.googleTruth, libs.googleTruthExtensions, libs.junit4).forEach { testImplementation(it) }

    listOf(
        libs.androidxTestCore,
        libs.androidxTestRunner,
        libs.androidxTestExtJunit,
        libs.androidxTestEspresso,
        libs.androidxCoreTesting,
        libs.androidxRoomTesting,
        libs.mockito,
        libs.googleTruth,
        libs.googleTruthExtensions,
        libs.junit4,
    ).forEach { androidTestImplementation(it) }

    ksp(libs.androidxRoomCompiler)
}
