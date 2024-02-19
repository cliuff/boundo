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

import android.content.pm.PackageManager
import android.util.Log
import com.madness.collision.util.sortedWithUtilsBy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

interface AppListRepository {
    val apps: Flow<List<CodingArtifact>>
    suspend fun fetchNewData(pm: PackageManager)
}

class AppListRepoImpl : AppListRepository {
    private val mutAppsFlow = MutableStateFlow(emptyList<CodingArtifact>())
    override val apps: Flow<List<CodingArtifact>> by ::mutAppsFlow

    override suspend fun fetchNewData(pm: PackageManager): Unit = supervisorScope {
        val platformList = async(Dispatchers.Default) { PlatformAppsFetcher.getRawList() }
        val shellList = async(Dispatchers.Default) { ShellAppsFetcher.getRawList() }
        platformList.invokeOnCompletion { Log.d("AppListViewModel", "PlatformAppListProvider/completed") }
        shellList.invokeOnCompletion { Log.d("AppListViewModel", "ShellAppListProvider/completed") }
        // todo save package info
        launch {
            val arts = platformList.await()
                .map { p -> ArtImpl(p.packageName, p.applicationInfo.loadLabel(pm).toString()) }
                .sortedWithUtilsBy(ArtImpl::label)
            mutAppsFlow.update { arts }
        }
        // todo incorporate shell apps
        launch {
            val arts = shellList.await()
                .map { p -> ArtImpl(p.packageName, "") }
                .sortedWithUtilsBy(ArtImpl::label)
            mutAppsFlow.update { arts }
        }
    }
}