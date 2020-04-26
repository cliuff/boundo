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

package com.madness.collision.unit.api_viewing.data

import android.content.pm.ApplicationInfo

internal class AppPackage(applicationInfo: ApplicationInfo) {

    val hasSplits: Boolean
    val apkPaths: List<String>
    init {
        val splitPaths = applicationInfo.splitPublicSourceDirs ?: emptyArray()
        hasSplits = splitPaths.isNotEmpty()
        val paths = ArrayList<String>(splitPaths.size + 1)
        paths.add(applicationInfo.publicSourceDir ?: "")
        paths.addAll(splitPaths)
        apkPaths = paths
    }

}
