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

package io.cliuff.boundo.wear.data

import android.content.Context
import io.cliuff.boundo.data.PackageInfoProvider
import io.cliuff.boundo.wear.data.model.toPkgApps
import io.cliuff.boundo.wear.model.ApiViewingApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal interface AppRepository {
    suspend fun getAppsOneShot(): List<ApiViewingApp>
}

internal class AppRepoImpl(
    private val pkgProvider: PackageInfoProvider,
    private val context: Context,
) : AppRepository {

    override suspend fun getAppsOneShot(): List<ApiViewingApp> {
        return withContext(Dispatchers.IO) {
            fetchAppsFromPlatform(context)
        }
    }

    private suspend fun fetchAppsFromPlatform(context: Context): List<ApiViewingApp> {
        val packages = pkgProvider.getAll()
        val apps = packages.toPkgApps(context)
        return apps
    }
}
