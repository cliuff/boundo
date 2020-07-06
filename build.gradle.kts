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

plugins {
//    "kotlin.dsl"
//    id("org.jetbrains.kotlin.kapt") version "1.3.20"
//    id("org.jetbrains.kotlin.android") version "1.3.20"
//    id("org.jetbrains.dokka-android") version "0.9.17"
}

buildscript {
    repositories {
        google()
        jcenter()
    }
    val versions = object {
        val kotlin = "1.3.72"
        val dokka = "0.9.18"
        val androidxNavigation = "2.3.0"
        val googlePlayServicesOSSLicensesPlugin = "0.10.0"
        val androidGradlePlugin = "4.0.0"
    }
    dependencies {
        classpath("com.android.tools.build:gradle:${versions.androidGradlePlugin}")
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:${versions.androidxNavigation}")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${versions.kotlin}")
        classpath("org.jetbrains.kotlin:kotlin-android-extensions:${versions.kotlin}")
        classpath("org.jetbrains.dokka:dokka-android-gradle-plugin:${versions.dokka}")
        classpath("com.google.android.gms:oss-licenses-plugin:${versions.googlePlayServicesOSSLicensesPlugin}")
    }
}

allprojects{
    repositories {
        google()
        jcenter()
    }
}

tasks{
    val clean by registering(Delete::class){
        delete(rootProject.buildDir)
    }
}

