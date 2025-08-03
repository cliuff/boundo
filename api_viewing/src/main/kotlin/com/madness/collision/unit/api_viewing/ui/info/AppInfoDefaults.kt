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

package com.madness.collision.unit.api_viewing.ui.info

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.madness.collision.unit.api_viewing.apps.AppRepo
import com.madness.collision.unit.api_viewing.apps.AppRepository
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.list.AppListService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Stable
class AppInfoSheetState(var pkgName: String?, app: ApiViewingApp?) {
    private var appRepo: AppRepository? = null
    private var mutApp = mutableStateOf(app)

    var app: ApiViewingApp?
        get() = mutApp.value
        set(value) {
            pkgName = value?.packageName
            mutApp.value = value
        }

    suspend fun restoreApp(context: Context) {
        val pkg = pkgName ?: return
        val restored = withContext(Dispatchers.IO) { getApp(context, pkg) }
        if (restored != null) app = restored
    }

    private fun getApp(context: Context, pkgName: String): ApiViewingApp? {
        val repo = appRepo ?: AppRepo.dumb(context).also { appRepo = it }
        return repo.getApp(pkgName)
    }

    companion object {
        fun Saver() =
            Saver<AppInfoSheetState, String>(
                save = { it.pkgName },
                restore = { pkgName ->
                    AppInfoSheetState(pkgName, null)
                }
            )
    }
}

@Composable
fun rememberAppInfoState(initApp: ApiViewingApp? = null): AppInfoSheetState {
    val state = rememberSaveable(initApp, saver = AppInfoSheetState.Saver()) {
        AppInfoSheetState(initApp?.packageName, initApp)
    }
    // restore app from saved package name
    if (state.app == null && state.pkgName != null) {
        val context = LocalContext.current
        LaunchedEffect(state) {
            state.restoreApp(context)
        }
    }
    return state
}

@Composable
fun rememberAppInfoEventHandler(hostFragment: Fragment) =
    hostFragment.run {
        remember<AppInfoEventHandler> {
            object : AppInfoEventHandler {

                override fun shareAppIcon(app: ApiViewingApp) {
                    val context = context ?: return
                    AppListService().actionIcon(context, app, parentFragmentManager)
                }

                override fun shareAppArchive(app: ApiViewingApp) {
                    val context = context ?: return
                    AppListService().actionApk(context, app, lifecycleScope, parentFragmentManager)
                }
            }
        }
    }
