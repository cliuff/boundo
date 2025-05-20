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
}

android {
    sourceSets {
        getByName("main").java.srcDir("src/main/kotlin")
    }
    // namespace is used by R and BuildConfig classes
    namespace = "com.madness.collision.unit.api_viewing"
    compileSdk = 36

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
            proguardFile("proguard-rules.pro")
        }
        getByName("release") {
            isJniDebuggable = false
            renderscriptOptimLevel = 3
            proguardFile("proguard-rules.pro")
        }
        // FOSS release
        create("foss") {
            // foss inherits from release build
            initWith(getByName("release"))
            // match the release build type for submodules
            matchingFallbacks += "release"
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
    implementation(project(":mods:org"))
    implementation(platform(libs.androidxComposeBom))
    listOf(
        // commented out: manually include libs conditionally for build types
        // fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))),
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

    // include Mipush SDK in pro builds (non-foss i.e. debug/release)
    releaseImplementation(files("libs/MiPush_SDK_Client_6_0_1-C.jar"))
    debugImplementation(files("libs/MiPush_SDK_Client_6_0_1-C.jar"))

    debugImplementation(libs.androidxComposeUiTooling)

    listOf(libs.mockito, libs.googleTruth, libs.googleTruthExtensions, libs.junit4).forEach { testImplementation(it) }

    ksp(libs.androidxRoomCompiler)
}
