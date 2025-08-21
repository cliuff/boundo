/*
 * Copyright 2025 Clifford Liu
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

package io.cliuff.boundo.wear.data.model

import android.content.Context
import android.content.pm.PackageInfo
import io.cliuff.boundo.wear.model.ApiViewingApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/** Convert to [ApiViewingApp]s and [init()][ApiViewingApp.init]. */
internal suspend fun List<PackageInfo>.toPkgApps(context: Context) = coroutineScope {
    val arr = Array(size) { i ->
        async(Dispatchers.IO) {
            ApiViewingApp(context, get(i))
        }
    }
    // use array to avoid toTypedArray() conversion
    awaitAll(*arr)
}
