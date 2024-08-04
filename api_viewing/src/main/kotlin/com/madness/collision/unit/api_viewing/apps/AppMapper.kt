/*
 * Copyright 2024 Clifford Liu
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

package com.madness.collision.unit.api_viewing.apps

import android.content.Context
import android.content.pm.PackageInfo
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/** Convert to [ApiViewingApp]s and [init()][ApiViewingApp.init]. */
suspend fun List<PackageInfo>.toPkgApps(context: Context, anApp: ApiViewingApp) = coroutineScope {
    // clone apps all at once, potentially faster?
    val apps = Array(size) { i -> if (i == lastIndex) anApp else anApp.clone() as ApiViewingApp }
    val arr = Array(size) { i ->
        async(Dispatchers.IO) {
            apps[i].apply { init(context, get(i), preloadProcess = true, archive = false) }
        }
    }
    // use array to avoid toTypedArray() conversion
    awaitAll(*arr)
}
