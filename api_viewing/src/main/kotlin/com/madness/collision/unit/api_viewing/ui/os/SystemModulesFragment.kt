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

package com.madness.collision.unit.api_viewing.ui.os

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.Toolbar
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madness.collision.Democratic
import com.madness.collision.chief.app.ComposeFragment
import com.madness.collision.chief.app.rememberColorScheme
import com.madness.collision.unit.api_viewing.R
import com.madness.collision.unit.api_viewing.data.ModuleInfo
import com.madness.collision.unit.api_viewing.info.PkgInfo
import com.madness.collision.unit.api_viewing.ui.info.rememberAppInfoEventHandler
import com.madness.collision.util.os.OsUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SystemModulesFragment : ComposeFragment(), Democratic {
    override val category: String = "AV"
    override val id: String = "SystemModules"

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        mainViewModel.configNavigation(toolbar, iconColor)
        toolbar.setTitle(R.string.av_osmod_title)
        return true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        democratize(mainViewModel)
    }

    @Composable
    override fun ComposeContent() {
        MaterialTheme(colorScheme = rememberColorScheme()) {
            SystemModulesPage(
                appInfoEventHandler = rememberAppInfoEventHandler(this),
                contentPadding = rememberContentPadding(),
            )
        }
    }
}

class ApkInfo(val packageName: String, val isApex: Boolean)

typealias SystemModulesUiState = Pair<List<ModuleInfo>, Map<String, ApkInfo>>

class SystemModulesViewModel : ViewModel() {
    private val mutUiState: MutableState<SystemModulesUiState> =
        mutableStateOf(emptyList<ModuleInfo>() to emptyMap())
    val uiState: State<SystemModulesUiState> by ::mutUiState

    fun init(context: Context) {
        if (OsUtils.dissatisfy(OsUtils.Q)) return
        viewModelScope.launch(Dispatchers.IO) {
            val moduleList = PkgInfo.getMetadataModules(context)
                .sortedWith(compareBy(ModuleInfo::name, ModuleInfo::pkgName))
            mutUiState.value = moduleList to getApkInfoMap(moduleList, context)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getApkInfoMap(modules: List<ModuleInfo>, context: Context): Map<String, ApkInfo> {
        if (modules.isEmpty()) return emptyMap()
        val modPkgSet = modules.mapNotNullTo(HashSet(modules.size)) { it.pkgName }
        return PkgInfo.getApexIncludedPackages(context)
            .filter { it.packageName in modPkgSet }
            .map { ApkInfo(it.packageName, it.isApex) }
            .associateBy { it.packageName }
    }
}
