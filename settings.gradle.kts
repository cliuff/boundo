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


includeBuild("dependencyPlugin")
include(":app", ":api_viewing", ":wearable", ":apk-parser")
project(":apk-parser").projectDir = File("subproject/apk-parser")
/*

pluginManagement {
    repositories {
        gradlePluginPortal()
        jcenter()
        google()
    }
    resolutionStrategy {
        eachPlugin {
            val verBuildTools = "3.3.0"
            val verNavigation = "1.0.0-alpha10"
            if (requested.id.namespace == "com.android") {
                useModule("com.android.tools.build:gradle:$verBuildTools")
            }
            if (requested.id.namespace == "androidx.navigation") {
                useModule("android.arch.navigation:navigation-safe-args-gradle-plugin:$verNavigation")
            }
        }
    }
}*/
