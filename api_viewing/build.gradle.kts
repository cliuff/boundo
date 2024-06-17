import com.cliuff.boundo.build.configLibCheckerRules

plugins {
    alias(libs.plugins.android.dynamic.feature)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.ksp)
    // implement parcelable interface by using annotation
    id("kotlin-parcelize")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "false")
}

android {
    sourceSets {
        getByName("main").java.srcDir("src/main/kotlin")
    }
    // namespace is used by R and BuildConfig classes
    namespace = "com.madness.collision.unit.api_viewing"
    compileSdk = 34

    defaultConfig {
        minSdk = 23
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        configLibCheckerRules(libs.libCheckerRules) { ver ->
            val javaValue = if (ver != null) "\"$ver\"" else "null"
            buildConfigField("String", "LIBCHECKER_RULES_VER", javaValue)
        }
    }
    buildTypes {
        getByName("debug") {
            isJniDebuggable = false
            renderscriptOptimLevel = 3
        }
        getByName("release") {
            isJniDebuggable = false
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
    buildFeatures.buildConfig = true
    buildFeatures.viewBinding = true
    buildFeatures.compose = true
}

dependencies {
    coreLibraryDesugaring(libs.androidDesugaring)
    implementation(platform(libs.androidxComposeBom))
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
        libs.haze,
        libs.androidxConstraintLayoutCompose,
        libs.libCheckerRules,
        libs.google.smali,
        libs.ldapsdk,
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
