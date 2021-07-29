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
import com.cliuff.boundo.dependency.Dependencies

plugins {
    id("com.android.application")
    kotlin("android")
    id("androidx.navigation.safeargs")
    id("dagger.hilt.android.plugin")
    id("kotlin-kapt")
//    id("org.jetbrains.dokka")
//    id("org.jetbrains.dokka-android")
    id("com.google.android.gms.oss-licenses-plugin")
    id("com.cliuff.boundo.dependencies")
}

// below: load the desired values from custom.properties in order to be injected into BuildConfig and Res
// the values in custom.properties must not contain quotes otherwise the parsed values here will contain quotes
val properties = Properties()
properties.load(project.rootProject.file("custom.properties").inputStream())
val buildPackage: String = properties.getProperty("packageName", "")
val signingKeyStorePath: String = properties.getProperty("signingKeyStorePath", "")
val signingKeyStorePassword: String = properties.getProperty("signingKeyStorePassword", "")
val signingKeyAlias: String = properties.getProperty("signingKeyAlias", "")
val signingKeyPassword: String = properties.getProperty("signingKeyPassword", "")

//dokkaHtml {
//    outputDirectory = "$buildDir/dokka"
//    dokkaSourceSets {
//        create("main") {
//            noAndroidSdkLink = true
//        }
//    }
//}

android {
    buildToolsVersion = "30.0.3"
    sourceSets {
        getByName("main").java.srcDir("src/main/kotlin")
    }
    signingConfigs {
        create("Sign4Release") {
            keyAlias = signingKeyAlias
            keyPassword = signingKeyPassword
            storeFile = file(signingKeyStorePath)
            storePassword = signingKeyStorePassword
        }
    }
    compileSdkVersion(30)
    defaultConfig {
        // below: manifest placeholders
        manifestPlaceholders["buildPackage"] = buildPackage
        applicationId = "com.madness.collision"
        minSdkVersion(22)
        targetSdkVersion(30)
        versionCode = 21052716
        versionName = "3.7.5"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testApplicationId = "${applicationId}.test"
        renderscriptSupportModeEnabled = true
        signingConfig = signingConfigs.getByName("Sign4Release")
        kapt {
            arguments {
                arg("room.schemaLocation", "$projectDir/schemas")
            }
        }
        // below: inject the desired values into BuildConfig and Res
        // the string values have to be wrapped in quotes because the value in local.properties does not have quotes
        buildConfigField("String", "BUILD_PACKAGE", "\"$buildPackage\"")
        resValue("string", "buildPackage", buildPackage)
        // below: fix multi-locale support
        resConfigs("en", "en-rGB", "en-rUS",
                "zh", "zh-rCN", "zh-rHK", "zh-rMO", "zh-rTW", "zh-rSG",
                "ru", "ru-rRU", "es", "es-rES", "es-rUS",
                "ar", "it", "it-rIT", "pt", "pt-rPT",
                "th", "th-rTH", "vi", "vi-rVN",
                "fr", "fr-rFR", "el", "el-rGR",
                "ja", "ja-rJP", "ko", "ko-rKR",
                "tr", "tr-rTR", "de", "de-rDE"
        )
    }
    flavorDimensions("arch")
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
            signingConfig = signingConfigs.getByName("Sign4Release")
            isRenderscriptDebuggable = false
            renderscriptOptimLevel = 3
            isZipAlignEnabled = true
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        getByName("release") {
            isDebuggable = false
            isJniDebuggable = false
            signingConfig = signingConfigs.getByName("Sign4Release")
            isRenderscriptDebuggable = false
            renderscriptOptimLevel = 3
            isZipAlignEnabled = true
            isMinifyEnabled = true
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        // Flag to enable support for the new Java 8+ APIs
        isCoreLibraryDesugaringEnabled = true
        targetCompatibility = JavaVersion.VERSION_1_8
        sourceCompatibility = JavaVersion.VERSION_1_8
    }
    lintOptions {
        isCheckReleaseBuilds = false
        // Or, if you prefer, you can continue to check for errors in release builds,
        // but continue the build even when errors are found:
        isAbortOnError = false
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
    kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
    buildFeatures {
        dataBinding = true
        viewBinding = true
    }
    dynamicFeatures = mutableSetOf(
        ":image_modifying", ":school_timetable", ":api_viewing", ":qq_contacts"
    )
}

dependencies {

    coreLibraryDesugaring(Dependencies.androidDesugaring)

    listOf(
            fileTree(Dependencies.fileTreeValue),
//            Dependencies.androidxWorkRuntime,
//            Dependencies.androidxWorkFirebase,
            Dependencies.androidxCore,
            Dependencies.androidxCoreKtx,
            Dependencies.androidxAppcompat,
            Dependencies.androidxFragment,
            Dependencies.androidxDrawerLayout,
            Dependencies.androidxSwipeRefreshLayout,
            Dependencies.androidxConstraintLayout,
            Dependencies.androidxPalette,
            Dependencies.androidxCardView,
            Dependencies.androidxRecyclerView,
            Dependencies.androidxViewPager,
            Dependencies.androidxLifecycleCommon,
            Dependencies.androidxLifecycleViewModel,
            Dependencies.androidxLifecycleLiveData,
            Dependencies.androidxPaging,
            Dependencies.androidxPreference,
            Dependencies.androidxNavigationFragment,
            Dependencies.androidxNavigationUI,
            Dependencies.androidxDocumentFile,
            Dependencies.gglHilt,
            Dependencies.googleMaterialComponents,
            Dependencies.googlePlayServicesOSSLicenses,
            Dependencies.googleGson,
            Dependencies.googlePlayCore,
            Dependencies.googlePlayCoreKtx,
            Dependencies.gglGuava,
            Dependencies.jsoup,
            Dependencies.kotlinStdlib,
            Dependencies.kotlinReflect,
            Dependencies.rxJava,
            Dependencies.jbAnnotations,
            Dependencies.glide,
            Dependencies.coil,
            Dependencies.androidDeviceNames
    ).forEach { implementation(it) }

    kapt(Dependencies.gglHiltCompiler)

    listOf(
            Dependencies.mockito,
            Dependencies.googleTruth,
            Dependencies.googleTruthExtensions,
            Dependencies.junit4
    ).forEach { testImplementation(it) }

    listOf(
            Dependencies.androidxTestCore,
            Dependencies.androidxTestRunner,
            Dependencies.androidxTestExtJunit,
            Dependencies.androidxTestEspresso,
            Dependencies.androidxCoreTesting,
            Dependencies.androidxRoomTesting,
//            Dependencies.androidxNavigationTesting,
//            Dependencies.androidxWorkTesting,
            Dependencies.mockito,
            Dependencies.googleTruth,
            Dependencies.googleTruthExtensions,
            Dependencies.junit4
    ).forEach { androidTestImplementation(it) }

    api(Dependencies.kotlinCoroutines)

}
