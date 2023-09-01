/*
 * Copyright 2021 Clifford Liu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import java.util.*

plugins {
    id("com.android.application")
    kotlin("android")
    alias(libs.plugins.google.ksp)
    // implement parcelable interface by using annotation
    id("kotlin-parcelize")
//    id("org.jetbrains.dokka")
//    id("org.jetbrains.dokka-android")
    id("com.google.android.gms.oss-licenses-plugin")
}

// below: load the desired values from custom.properties in order to be injected into BuildConfig and Res
// the values in custom.properties must not contain quotes otherwise the parsed values here will contain quotes
val properties = Properties()
val configSigning = try {
    val customPropFile = project.rootProject.file("custom.properties")
    properties.load(customPropFile.inputStream())
    true
} catch (ignored: Exception) {
    false
}
val prop: (key: String, defValue: String) -> String = { key, defValue ->
    if (configSigning) properties.getProperty(key, defValue) else defValue
}
val buildPackage: String = prop("packageName", "com.madness.collision")
val signingKeyStorePath: String = prop("signingKeyStorePath", "")
val signingKeyStorePassword: String = prop("signingKeyStorePassword", "")
val signingKeyAlias: String = prop("signingKeyAlias", "")
val signingKeyPassword: String = prop("signingKeyPassword", "")

//dokkaHtml {
//    outputDirectory = "$buildDir/dokka"
//    dokkaSourceSets {
//        create("main") {
//            noAndroidSdkLink = true
//        }
//    }
//}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    buildToolsVersion = "34.0.0"
    sourceSets {
        getByName("main").java.srcDir("src/main/kotlin")
    }
    signingConfigs {
        if (configSigning) {
            create("Sign4Release") {
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
                storeFile = file(signingKeyStorePath)
                storePassword = signingKeyStorePassword
            }
        }
    }
    // namespace is used by R and BuildConfig classes
    namespace = "com.madness.collision"
    compileSdk = 34
    defaultConfig {
        // below: manifest placeholders
        manifestPlaceholders["buildPackage"] = buildPackage
        applicationId = "com.madness.collision"
        minSdk = 23
        targetSdk = 34
        versionCode = 23081415
        versionName = "4.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testApplicationId = "${applicationId}.test"
        renderscriptSupportModeEnabled = true
        if (configSigning) {
            signingConfig = signingConfigs.getByName("Sign4Release")
        }
        // below: inject the desired values into BuildConfig and Res
        // the string values have to be wrapped in quotes because the value in local.properties does not have quotes
        buildConfigField("String", "BUILD_PACKAGE", "\"$buildPackage\"")
        // override with current time in release builds to increase performance during debugging
        buildConfigField("long", "BUILD_TIMESTAMP", "0")
        resValue("string", "buildPackage", buildPackage)
        // below: fix multi-locale support
        resourceConfigurations.addAll(arrayOf(
            "en", "en-rGB", "en-rUS",
            "zh", "zh-rCN", "zh-rHK", "zh-rMO", "zh-rTW", "zh-rSG",
            "ru", "ru-rRU", "uk", "uk-rUA", "es", "es-rES", "es-rUS",
            "ar", "it", "it-rIT", "pt", "pt-rPT",
            "th", "th-rTH", "vi", "vi-rVN",
            "fr", "fr-rFR", "el", "el-rGR",
            "ja", "ja-rJP", "ko", "ko-rKR",
            "tr", "tr-rTR", "de", "de-rDE",
            "bn", "bn-rBD", "fa", "fa-rAF", "fa-rIR",
            "hi", "hi-rIN", "in", "in-rID",
            "mr", "mr-rIN", "pa", "pa-rPK",
            "pl", "pl-rPL",
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
            renderscriptOptimLevel = 3
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
            renderscriptOptimLevel = 3
            isMinifyEnabled = true
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // override field with current time in release builds only
            buildConfigField("long", "BUILD_TIMESTAMP", System.currentTimeMillis().toString())
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
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
    buildFeatures {
        buildConfig = true
        dataBinding = true
        viewBinding = true
        compose = true
    }
    composeOptions {
        // Jetpack Compose compiler version
        kotlinCompilerExtensionVersion = libs.versions.androidxComposeCompiler.get()
    }
    dynamicFeatures.add(":api_viewing")
    bundle {
        // disable split apks for languages to better support in-app language switching,
        // for language resources occupy a little space and implementing on-demand language downloads is tedious
        language.enableSplit = false
    }
}

repositories {
    // required by SmoothCornerShape
    maven { url = uri("https://jitpack.io") }
}

dependencies {

    coreLibraryDesugaring(libs.androidDesugaring)

    listOf(
        fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))),
//            libs.androidxWorkRuntime,
//            libs.androidxWorkFirebase,
        libs.androidxCore,
        libs.androidxCoreKtx,
        libs.androidxComposeRuntimeLiveData,
        libs.androidxComposeFoundation,
        libs.androidxComposeUi,
        libs.androidxComposeActivity,
        libs.androidxComposeMaterial3,
        libs.androidxComposeMaterialIcons,
        libs.androidxComposeMaterialIconsExtended,
        libs.androidxComposeAnimation,
        libs.androidxComposeUiTooling,
        libs.androidxComposeViewModel,
        libs.androidxComposeUiTest,
        libs.androidxActivity,
        libs.androidxAppcompat,
        libs.androidxFragment,
        libs.androidxWindow,
        libs.androidxDrawerLayout,
        libs.androidxSwipeRefreshLayout,
        libs.androidxConstraintLayout,
        libs.androidxPalette,
        libs.androidxCardView,
        libs.androidxRecyclerView,
        libs.androidxViewPager,
        libs.androidxLifecycleRuntime,
        libs.androidxLifecycleCommon,
        libs.androidxLifecycleViewModel,
        libs.androidxLifecycleLiveData,
        libs.androidxPaging,
        libs.androidxPreference,
        libs.androidxNavigationFragment,
        libs.androidxNavigationUI,
        libs.androidxDocumentFile,
        libs.androidxHeifWriter,
        libs.googleMaterialComponents,
        libs.googlePlayServicesOSSLicenses,
        libs.googleGson,
        libs.googlePlayFeatureDelivery,
        libs.googlePlayFeatureDeliveryKtx,
        libs.gglGuava,
        libs.jsoup,
        libs.kotlinStdlib,
        libs.kotlinReflect,
        libs.rxJava,
        libs.jbAnnotations,
        libs.okhttp,
        libs.coil,
        libs.coilCompose,
        libs.androidDeviceNames,
        libs.appIconLoader,
        libs.smoothCornerCompose,
    ).forEach { implementation(it) }

    listOf(libs.mockito, libs.googleTruth, libs.googleTruthExtensions, libs.junit4).forEach { testImplementation(it) }

    listOf(
        libs.androidxTestCore,
        libs.androidxTestRunner,
        libs.androidxTestExtJunit,
        libs.androidxTestEspresso,
        libs.androidxCoreTesting,
        libs.androidxRoomTesting,
//            libs.androidxNavigationTesting,
//            libs.androidxWorkTesting,
        libs.mockito,
        libs.googleTruth,
        libs.googleTruthExtensions,
        libs.junit4,
    ).forEach { androidTestImplementation(it) }

    api(libs.kotlinCoroutines)

}
