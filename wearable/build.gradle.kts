import com.cliuff.boundo.build.getCustomConfig

plugins {
    alias(libs.plugins.android)
    alias(libs.plugins.kotlin.android)
}

android {
    buildToolsVersion = "34.0.0"
    sourceSets {
        getByName("main").java.srcDir("src/main/kotlin")
    }
    val customConfig = getCustomConfig(project)
    val buildPackage = customConfig.buildPackage
    val configSigning = customConfig.signing != null
    signingConfigs {
        customConfig.signing?.run {
            create("Sign4Release") {
                keyAlias = key.alias
                keyPassword = key.password
                storeFile = file(store.path)
                storePassword = store.password
            }
        }
    }
    // namespace is used by R and BuildConfig classes
    namespace = "com.madness.collision.wearable"
    compileSdk = 34
    defaultConfig {
        // below: manifest placeholders
        manifestPlaceholders["buildPackage"] = buildPackage
        applicationId = "com.madness.collision"
        minSdk = 23
        targetSdk = 33
        versionCode = 24010122
        versionName = "4.1.4W"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testApplicationId = "${applicationId}.test"
        if (configSigning) {
            signingConfig = signingConfigs.getByName("Sign4Release")
        }
        // below: inject the desired values into BuildConfig and Res
        // the string values have to be wrapped in quotes because the value in local.properties does not have quotes
        buildConfigField("String", "BUILD_PACKAGE", "\"$buildPackage\"")
        resValue("string", "buildPackage", buildPackage)
        // below: fix multi-locale support
        resourceConfigurations.addAll(arrayOf(
            "en", "en-rGB", "en-rUS",
            "zh", "zh-rCN", "zh-rHK", "zh-rMO", "zh-rTW", "zh-rSG",
            "ru", "ru-rRU", "es", "es-rES", "es-rUS",
            "ar", "it", "it-rIT", "pt", "pt-rPT",
            "th", "th-rTH", "vi", "vi-rVN",
            "fr", "fr-rFR", "el", "el-rGR",
            "ja", "ja-rJP", "ko", "ko-rKR",
            "tr", "tr-rTR", "de", "de-rDE"
        ))
    }
    flavorDimensions.add("arch")
    productFlavors {
        create("full") {
            dimension = "arch"
        }
        create("arm") {
            dimension = "arch"
            ndk {
                abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a"))
            }
        }
        create("x86") {
            dimension = "arch"
            ndk {
                abiFilters.addAll(listOf("x86", "x86_64"))
            }
        }
    }
    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".mortal"
            isDebuggable = true
            isJniDebuggable = false
            if (configSigning) {
                signingConfig = signingConfigs.getByName("Sign4Release")
            }
            isRenderscriptDebuggable = false
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        getByName("release") {
            isDebuggable = false
            isJniDebuggable = false
            if (configSigning) {
                signingConfig = signingConfigs.getByName("Sign4Release")
            }
            isRenderscriptDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        // Flag to enable support for the new Java 8+ APIs
        isCoreLibraryDesugaringEnabled = true
        targetCompatibility = JavaVersion.VERSION_17
        sourceCompatibility = JavaVersion.VERSION_17
    }
    kotlin.jvmToolchain(17)
    packagingOptions {
        // The kotlinx-coroutines-core artifact contains a resource file
        // that is not required for the coroutines to operate normally
        // and is only used by the debugger
        resources.excludes.add("DebugProbesKt.bin")
    }
    lint {
        checkReleaseBuilds = false
        // Or, if you prefer, you can continue to check for errors in release builds,
        // but continue the build even when errors are found:
        abortOnError = false
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}

dependencies {
    coreLibraryDesugaring(libs.androidDesugaring)

    listOf(
        fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))),
        libs.androidxCore,
        libs.androidxCoreKtx,
        libs.androidxCoreSplashScreen,
        libs.androidxActivity,
        libs.androidxAppcompat,
        libs.androidxFragment,
        libs.androidxPalette,
        libs.androidxRecyclerView,
        libs.androidxViewPager,
        libs.androidxLifecycleRuntime,
        libs.androidxLifecycleCommon,
        libs.androidxLifecycleViewModel,
        libs.androidxLifecycleLiveData,
        libs.androidxPreference,
        libs.googleMaterialComponents,
        libs.kotlinStdlib,
        libs.androidxWear,
    ).forEach { implementation(it) }

    compileOnly(libs.googleWearable)

    api(libs.kotlinCoroutines)
}
