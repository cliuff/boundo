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

package com.madness.collision.unit.api_viewing.ui.org.coll

import android.content.Context
import android.content.pm.PackageInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madness.collision.unit.api_viewing.apps.PlatformAppProvider
import io.cliuff.boundo.org.data.repo.CompCollRepository
import io.cliuff.boundo.org.data.repo.OrgCollRepo
import io.cliuff.boundo.org.model.CompColl
import io.cliuff.boundo.org.model.OrgApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OrgCollUiState(
    val isLoading: Boolean,
    val coll: CompColl?,
)

class OrgCollViewModel : ViewModel() {
    private val mutUiState: MutableStateFlow<OrgCollUiState> =
        MutableStateFlow(OrgCollUiState(isLoading = false, coll = null))
    val uiState: StateFlow<OrgCollUiState> = mutUiState.asStateFlow()
    private var compCollRepo: CompCollRepository? = null
    private var groupPkgs: Map<String, PackageInfo> = emptyMap()

    fun init(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            mutUiState.update { it.copy(isLoading = true) }
            val repo = compCollRepo ?: OrgCollRepo.compColl(context).also { compCollRepo = it }
            repo.getCompCollection()
                .onEach { coll ->
                    groupPkgs = coll?.getGroupPkgs(context).orEmpty()
                    mutUiState.update { it.copy(isLoading = false, coll = coll) }
                }
                .launchIn(this)
        }
    }

    fun getPkg(pkgName: String): PackageInfo? {
        return groupPkgs[pkgName]
    }
}

private fun CompColl.getGroupPkgs(context: Context, limit: Int = 3): Map<String, PackageInfo> {
    val pkgSize = groups.sumOf { it.apps.size.coerceAtMost(limit) }
    if (pkgSize <= 0) return emptyMap()

    val names = groups.flatMapTo(LinkedHashSet(pkgSize)) { it.apps.take(limit).map(OrgApp::pkg) }
    val pkgs = PlatformAppProvider(context).getAll()
    return buildMap(pkgSize) {
        for (p in pkgs) {
            if (p.packageName in names) put(p.packageName, p)
            if (size >= pkgSize) break
        }
    }
}
