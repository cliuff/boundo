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

package com.cliuff.boundo.dependency

import org.gradle.api.Plugin
import org.gradle.api.Project

object Versions {
    const val androidxTestCore = "1.2.0"
    const val androidxTestRunner = "1.2.0"
    const val androidxTestExtJunit = "1.1.1"
    const val androidxTestEspresso = "3.2.0"

    const val androidxCore = "1.3.0"
    const val androidxAppcompat = "1.1.0"
    const val androidxLifecycle = "2.2.0"
    const val androidxRoom = "2.2.5"
    const val androidxNavigation = "2.3.0"
    const val androidxCoreTesing = "2.1.0"
    const val androidxFragment = "1.2.5"
    const val androidxDrawerLayout = "1.1.0"
    const val androidxSwipeRefreshLayout = "1.1.0"
    const val androidxConstraintLayout = "2.0.0-beta8"
    const val androidxPalette = "1.0.0"
    const val androidxCardView = "1.0.0"
    const val androidxRecyclerView = "1.1.0"
    const val androidxHeifWriter = "1.0.0"
    const val androidxDataBinding = "3.5.2"
    const val androidxViewPager = "1.0.0"
    const val androidxPaging = "2.1.2"
    const val androidxPreference = "1.1.1"
    const val androidxWork = "2.0.1"
    const val androidxDocumentFile = "1.0.1"
    const val androidxPercentLayout = "1.0.0"
    const val androidxLegacyV4 = "1.0.0"
    const val androidxWear = "1.0.0"

    const val googleTruth = "1.0"
    const val googleMaterialTheme = "1.1.0"
    const val googlePlayServicesOSSLicenses = "17.0.0"
    const val googlePlayServicesBasement = "17.2.1"
    const val googleGson = "2.8.6"
    const val googlePlayCore = "1.7.3"
    const val googleSupportWearable = "2.7.0"
    const val googleWearable = "2.7.0"
    const val glide = "4.9.0"
    const val jsoup = "1.12.1"
    const val junitJupiter = "5.5.2"
    const val kotlin = "1.3.72"
    const val kotlinCoroutines = "1.3.0"
    const val mpChart = "v3.1.0"
}

class Dependencies : Plugin<Project> {
    override fun apply(project: Project) {
        // Possibly common dependencies or can stay empty
    }

    companion object {
        // gradle plugins
        const val kotlinGradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
        const val kotlinAndroidExtensions = "org.jetbrains.kotlin:kotlin-android-extensions:${Versions.kotlin}"

        const val androidxTestCore = "androidx.test:core:${Versions.androidxTestCore}"
        const val androidxTestRunner = "androidx.test:runner:${Versions.androidxTestRunner}"
        const val androidxTestExtJunit = "androidx.test.ext:junit:${Versions.androidxTestExtJunit}"
        const val androidxTestEspresso = "androidx.test.espresso:espresso-core:${Versions.androidxTestEspresso}"

        const val androidxCore = "androidx.core:core:${Versions.androidxCore}"
        const val androidxCoreKtx = "androidx.core:core-ktx:${Versions.androidxCore}"
        const val androidxAppcompat = "androidx.appcompat:appcompat:${Versions.androidxAppcompat}"
        const val androidxFragment = "androidx.fragment:fragment-ktx:${Versions.androidxFragment}"
        const val androidxDrawerLayout = "androidx.drawerlayout:drawerlayout:${Versions.androidxDrawerLayout}"
        const val androidxSwipeRefreshLayout = "androidx.swiperefreshlayout:swiperefreshlayout:${Versions.androidxSwipeRefreshLayout}"
        const val androidxConstraintLayout = "androidx.constraintlayout:constraintlayout:${Versions.androidxConstraintLayout}"
        const val androidxPalette = "androidx.palette:palette:${Versions.androidxPalette}"
        const val androidxCardView = "androidx.cardview:cardview:${Versions.androidxCardView}"
        const val androidxRecyclerView = "androidx.recyclerview:recyclerview:${Versions.androidxRecyclerView}"
        const val androidxHeifWriter = "androidx.heifwriter:heifwriter:${Versions.androidxHeifWriter}"
        const val androidxDataBinding = "androidx.databinding:databinding-adapters:${Versions.androidxDataBinding}"
        const val androidxViewPager = "androidx.viewpager2:viewpager2:${Versions.androidxViewPager}"
        const val androidxDocumentFile = "androidx.documentfile:documentfile:${Versions.androidxDocumentFile}"

        const val androidxLifecycleCommon = "androidx.lifecycle:lifecycle-common-java8:${Versions.androidxLifecycle}"
        const val androidxLifecycleViewModel = "androidx.lifecycle:lifecycle-viewmodel-ktx:${Versions.androidxLifecycle}"
        const val androidxLifecycleLiveData = "androidx.lifecycle:lifecycle-livedata-ktx:${Versions.androidxLifecycle}"
        const val androidxCoreTesing = "androidx.arch.core:core-testing:${Versions.androidxCoreTesing}"
        const val androidxRoomRuntime = "androidx.room:room-runtime:${Versions.androidxRoom}"
        const val androidxRoomCompiler = "androidx.room:room-compiler:${Versions.androidxRoom}"
        const val androidxRoom = "androidx.room:room-ktx:${Versions.androidxRoom}"
        const val androidxRoomTesing = "androidx.room:room-testing:${Versions.androidxRoom}"
        const val androidxPaging = "androidx.paging:paging-runtime-ktx:${Versions.androidxPaging}"
        const val androidxPreference = "androidx.preference:preference-ktx:${Versions.androidxPreference}"
        const val androidxNavigationFragment = "androidx.navigation:navigation-fragment-ktx:${Versions.androidxNavigation}"
        const val androidxNavigationUI = "androidx.navigation:navigation-ui-ktx:${Versions.androidxNavigation}"
        const val androidxNavigationTesting = "androidx.navigation:navigation-testing:${Versions.androidxNavigation}"

        const val androidxWorkRuntime = "androidx.work:work-runtime-ktx:${Versions.androidxWork}"
        const val androidxWorkTesting = "androidx.work:work-testing:${Versions.androidxWork}"
        // optional - Firebase JobDispatcher support
        const val androidxWorkFirebase = "androidx.work:work-firebase:${Versions.androidxWork}"

        const val googleMaterialTheme = "com.google.android.material:material:${Versions.googleMaterialTheme}"
        const val googlePlayServicesOSSLicenses = "com.google.android.gms:play-services-oss-licenses:${Versions.googlePlayServicesOSSLicenses}"
        const val googlePlayServicesBasement = "com.google.android.gms:play-services-basement:${Versions.googlePlayServicesBasement}"
        const val glide = "com.github.bumptech.glide:glide:${Versions.glide}"
        const val jsoup = "org.jsoup:jsoup:${Versions.jsoup}"
        const val kotlinStdlib = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Versions.kotlin}"
        const val kotlinReflect = "org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}"

        const val googleTruth = "com.google.truth:truth:${Versions.googleTruth}"
        const val googleTruthExtensions = "com.google.truth.extensions:truth-java8-extension:${Versions.googleTruth}"
        const val googleGson = "com.google.code.gson:gson:${Versions.googleGson}"
        const val googlePlayCore = "com.google.android.play:core:${Versions.googlePlayCore}"
        const val junitJupiter = "org.junit.jupiter:junit-jupiter-api:${Versions.junitJupiter}"
        const val kotlinCoroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.kotlinCoroutines}"
        const val mpChart = "com.github.PhilJay:MPAndroidChart:${Versions.mpChart}"

        // wear
        const val androidxPercentLayout = "androidx.percentlayout:percentlayout:${Versions.androidxPercentLayout}"
        const val androidxLegacyV4 = "androidx.legacy:legacy-support-v4:${Versions.androidxLegacyV4}"
        const val androidxWear = "androidx.wear:wear:${Versions.androidxWear}"
        const val googleSupportWearable = "com.google.android.support:wearable:${Versions.googleSupportWearable}"
        const val googleWearable = "com.google.android.wearable:wearable:${Versions.googleWearable}"

        val fileTreeValue = mapOf("dir" to "libs", "include" to listOf("*.jar"))

        val dynamicFeatureBasics = listOf(
                androidxCore,
                androidxCoreKtx,
                androidxAppcompat,
                androidxFragment,
                androidxConstraintLayout,
                androidxLifecycleCommon,
                androidxLifecycleViewModel,
                androidxLifecycleLiveData,
                googleMaterialTheme,
                googlePlayCore
        )
    }
}
