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

package io.cliuff.boundo.wear.ui.list

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.cliuff.boundo.data.PlatformAppProvider
import io.cliuff.boundo.wear.data.AppRepoImpl
import io.cliuff.boundo.wear.data.AppRepository
import io.cliuff.boundo.wear.model.ApiViewingApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class AppListViewModel : ViewModel() {
    private var appRepo: AppRepository? = null
    private val mutAppListState = MutableStateFlow(emptyList<ApiViewingApp>())

    val appListState: StateFlow<List<ApiViewingApp>> = mutAppListState.asStateFlow()

    private var initJob: Job? = null

    fun init(context: Context) {
        if (initJob != null) return
        val pkgProvider = PlatformAppProvider(context)
        val repo = AppRepoImpl(pkgProvider, context)
        appRepo = repo

        initJob = viewModelScope.launch(Dispatchers.Default) {
            val comparator = compareByDescending(ApiViewingApp::updateTime)
                .thenBy(ApiViewingApp::name)
                .thenBy(ApiViewingApp::packageName)
            val appList = repo.getAppsOneShot().sortedWith(comparator)
            mutAppListState.update { appList }
        }
    }
}
