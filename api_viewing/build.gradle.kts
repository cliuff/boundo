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
import com.cliuff.boundo.dependency.Versions

plugins {
    id("com.android.dynamic-feature")
    kotlin("android")
    id("com.google.devtools.ksp") version "1.8.21-1.0.11"
    // implement parcelable interface by using annotation
    id("kotlin-parcelize")
    id("com.cliuff.boundo.dependencies")
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
    compileSdk = 33

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
        targetCompatibility = JavaVersion.VERSION_11
        sourceCompatibility = JavaVersion.VERSION_11
    }
    kotlin.jvmToolchain(11)
    buildFeatures.viewBinding = true
    buildFeatures.compose = true
    composeOptions {
        // Jetpack Compose compiler version
        kotlinCompilerExtensionVersion = Versions.androidxComposeCompiler
    }
}

repositories {
    // required by LibChecker-Rules-Bundle
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    Dependencies.run {
        coreLibraryDesugaring(androidDesugaring)
        listOf(
            fileTree(fileTreeValue),
            project(":app"),
            androidxDocumentFile,
            androidxSwipeRefreshLayout,
            androidxRecyclerView,
            androidxPreference,
            androidxRoom,
            androidxRoomRuntime,
            mpChart,
            openCsv,
            androidDeviceNames,
            smoothCornerCompose,
            androidxConstraintLayoutCompose,
            libCheckerRules,
            googleAccompanistFlowLayout,
            project(":apk-parser"),
        ).forEach { implementation(it) }
        dynamicFeatureBasics.forEach { implementation(it) }

        listOf(mockito, googleTruth, googleTruthExtensions, junit4).forEach { testImplementation(it) }

        listOf(
            androidxTestCore,
            androidxTestRunner,
            androidxTestExtJunit,
            androidxTestEspresso,
            androidxCoreTesting,
            androidxRoomTesting,
            mockito,
            googleTruth,
            googleTruthExtensions,
            junit4
        ).forEach { androidTestImplementation(it) }

        ksp(androidxRoomCompiler)
    }
}
