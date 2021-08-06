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


import com.cliuff.boundo.dependency.Dependencies

plugins {
    id("com.android.dynamic-feature")
    kotlin("android")
    id("kotlin-kapt")
    id("com.cliuff.boundo.dependencies")
}

android {
    sourceSets {
        getByName("main").java.srcDir("src/main/kotlin")
    }
    compileSdkVersion(30)

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
        minSdk = 22
    }

    compileOptions {
        // Flag to enable support for the new Java 8+ APIs
        isCoreLibraryDesugaringEnabled = true
        targetCompatibility = JavaVersion.VERSION_11
        sourceCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions.jvmTarget = JavaVersion.VERSION_11.toString()
    buildFeatures.viewBinding = true
}

dependencies {
    coreLibraryDesugaring(Dependencies.androidDesugaring)
    listOf(
            fileTree(Dependencies.fileTreeValue),
            project(":app"),
            Dependencies.googlePlayServicesBasement,
            Dependencies.jsoup
    ).forEach { implementation(it) }
    Dependencies.dynamicFeatureBasics.forEach { implementation(it) }

    listOf(
            Dependencies.googleTruth,
            Dependencies.googleTruthExtensions,
            Dependencies.junit4
    ).forEach { testImplementation(it) }

    listOf(
            Dependencies.googleTruth,
            Dependencies.googleTruthExtensions,
            Dependencies.junit4
    ).forEach { androidTestImplementation(it) }
}
