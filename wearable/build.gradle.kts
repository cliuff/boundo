/*
 * Copyright 2020 Clifford Liu
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
    kotlin("android.extensions")
    id("kotlin-kapt")
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

//dokka-android {
//    outputFormat = "html"
//    outputDirectory = "./kdoc"
//}

android {
    buildToolsVersion = "29.0.3"
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
    compileSdkVersion(29)
    defaultConfig {
        // below: manifest placeholders
        manifestPlaceholders = mapOf<String, String>("buildPackage" to buildPackage)
        applicationId = "com.madness.collision"
        minSdkVersion(23)
        targetSdkVersion(29)
        versionCode = 20012223
        versionName = "3.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testApplicationId = "${applicationId}.test"
        signingConfig = signingConfigs.getByName("Sign4Release")
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
            setDimension("arch")
        }
        create("arm") {
            setDimension("arch")
            ndk{
                abiFilters("armeabi-v7a", "arm64-v8a")
            }
        }
        create("x86") {
            setDimension("arch")
            ndk{
                abiFilters("x86", "x86_64")
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
            isZipAlignEnabled = true
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        targetCompatibility = JavaVersion.VERSION_1_8
        sourceCompatibility = JavaVersion.VERSION_1_8
    }
    lintOptions {
        isCheckReleaseBuilds = false
        // Or, if you prefer, you can continue to check for errors in release builds,
        // but continue the build even when errors are found:
        isAbortOnError = false
    }
    kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
    buildFeatures {
        dataBinding = true
        viewBinding = true
    }
}

dependencies {

    listOf(
            fileTree(Dependencies.fileTreeValue),
            Dependencies.androidxCore,
            Dependencies.androidxCoreKtx,
            Dependencies.androidxAppcompat,
            Dependencies.androidxFragment,
            Dependencies.androidxPalette,
            Dependencies.androidxRecyclerView,
            Dependencies.androidxViewPager,
            Dependencies.androidxLifecycleCommon,
            Dependencies.androidxLifecycleViewModel,
            Dependencies.androidxLifecycleLiveData,
            Dependencies.androidxPreference,
            Dependencies.googleMaterialTheme,
            Dependencies.kotlinStdlib,
            Dependencies.androidxPercentLayout,
            Dependencies.androidxLegacyV4,
            Dependencies.androidxWear,
            Dependencies.googleSupportWearable
    ).forEach { implementation(it) }

    compileOnly(Dependencies.googleWearable)

    api(Dependencies.kotlinCoroutines)

}
