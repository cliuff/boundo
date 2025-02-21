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

package com.madness.collision.unit.api_viewing.data

import android.content.Context
import com.madness.collision.unit.api_viewing.database.AppEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

fun ApiViewingApp.toEntity() =
    AppEntity(
        packageName = packageName,
        verName = verName,
        verCode = verCode,
        targetAPI = targetAPI,
        minAPI = minAPI,
        apiUnit = apiUnit,
        updateTime = updateTime,
        isNativeLibrariesRetrieved = isNativeLibrariesRetrieved,
        nativeLibraries = nativeLibraries,
        isLaunchable = isLaunchable,
        appPackage = appPackage,
        dexPackageFlags = dexPackageFlags.value,
        iconInfo = iconInfo
    )

fun AppEntity.toEntityApp(app: ApiViewingApp = ApiViewingApp()): ApiViewingApp {
    app.packageName = packageName
    app.verName = verName
    app.verCode = verCode
    app.targetAPI = targetAPI
    app.minAPI = minAPI
    app.apiUnit = apiUnit
    app.updateTime = updateTime
    app.isNativeLibrariesRetrieved = isNativeLibrariesRetrieved
    app.nativeLibraries = nativeLibraries
    app.isLaunchable = isLaunchable
    app.appPackage = appPackage
    app.dexPackageFlags = DexPackageFlags(dexPackageFlags)
    app.iconInfo = iconInfo
    return app
}


fun List<ApiViewingApp>.toEntities() = map(ApiViewingApp::toEntity)


/** Convert to [ApiViewingApp]s. */
fun List<AppEntity>.toEntityApps(anApp: ApiViewingApp): List<ApiViewingApp> {
    // clone apps all at once, potentially faster?
    val apps = Array(size) { i -> if (i == lastIndex) anApp else anApp.clone() as ApiViewingApp }
    return mapIndexed { i, ent -> ent.toEntityApp(apps[i]) }
}

/** Convert to [ApiViewingApp]s and [initIgnored()][ApiViewingApp.initIgnored]. */
suspend fun List<AppEntity>.toFullApps(anApp: ApiViewingApp, context: Context) = coroutineScope {
    // clone apps all at once, potentially faster?
    val apps = Array(size) { i -> if (i == lastIndex) anApp else anApp.clone() as ApiViewingApp }
    val arr = Array(size) { i ->
        async(Dispatchers.IO) {
            // get application info one at a time, may be optimized
            get(i).toEntityApp(apps[i]).apply { initIgnored(context) }
        }
    }
    // use array to avoid toTypedArray() conversion
    awaitAll(*arr)
}
