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
import android.os.Environment
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madness.collision.chief.app.SavedStateDelegate
import com.madness.collision.unit.api_viewing.ui.org.OrgPkgInfoProvider
import io.cliuff.boundo.org.data.io.OrgCollExport
import io.cliuff.boundo.org.data.repo.CompCollRepository
import io.cliuff.boundo.org.data.repo.OrgCollRepo
import io.cliuff.boundo.org.model.CompColl
import io.cliuff.boundo.org.model.OrgApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CollSavedState(savedState: SavedStateHandle) {
    private val stateDelegate = SavedStateDelegate(savedState)
    var selCollId: Int? by stateDelegate
}

data class OrgCollUiState(
    val isLoading: Boolean,
    val coll: CompColl?,
    val collList: List<CompColl>,
    val installedPkgsSummary: Pair<Int, Int>?,
)

class OrgCollViewModel(savedState: SavedStateHandle) : ViewModel() {
    private val mutUiState: MutableStateFlow<OrgCollUiState> =
        MutableStateFlow(OrgCollUiState(isLoading = false, coll = null, collList = emptyList(), installedPkgsSummary = null))
    val uiState: StateFlow<OrgCollUiState> = mutUiState.asStateFlow()
    private var compCollRepo: CompCollRepository? = null
    private var groupPkgs: Map<String, PackageInfo> = emptyMap()
    private var selCollJob: Job? = null

    private val savedObj: CollSavedState = CollSavedState(savedState)
    private var initJob: Job? = null

    fun init(context: Context) {
        if (initJob != null) return
        initJob = viewModelScope.launch(Dispatchers.IO) {
            mutUiState.update { it.copy(isLoading = true) }
            val repo = compCollRepo ?: OrgCollRepo.compColl(context).also { compCollRepo = it }
            repo.getCompCollections()
                .onEach { collList ->
                    mutUiState.update { it.copy(collList = collList) }
                    if (uiState.value.coll == null) {
                        val selColl = savedObj.selCollId
                            ?.let { cid -> collList.find { it.id == cid } }
                        (selColl ?: collList.firstOrNull())?.let { coll ->
                            selectColl(coll, context)
                        }
                    }
                }
                .launchIn(this)
        }
    }

    fun getPkg(pkgName: String): PackageInfo? {
        return groupPkgs[pkgName]
    }

    fun selectColl(coll: CompColl, context: Context) {
        val repo = compCollRepo ?: return
        selCollJob?.cancel()
        savedObj.selCollId = coll.id
        repo.getCompCollection(coll.id)
            .onEach { co ->
                groupPkgs = co?.getGroupPkgs(context).orEmpty()
                val collPkgs = co?.groups.orEmpty()
                    .run { flatMapTo(HashSet(size)) { it.apps.map(OrgApp::pkg) } }
                val installedPkgs = OrgPkgInfoProvider.getAll()
                    .run { mapTo(HashSet(size), PackageInfo::packageName) }
                val intersectPkgs = collPkgs.apply { retainAll(installedPkgs) }
                val summary = intersectPkgs.size to installedPkgs.size
                mutUiState.update { it.copy(isLoading = false, coll = co, installedPkgsSummary = summary) }
            }
            .flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)
            .also { selCollJob = it }
    }

    fun deleteColl(coll: CompColl) {
        val repo = compCollRepo ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val isOk = repo.removeCompCollection(coll)
        }
    }

    fun importColl(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            kotlin.runCatching {
                val file = context.externalCacheDir?.resolve("Import.org.txt")
                if (file != null && file.exists()) OrgCollExport.fromStream(file.inputStream(), context)
            }.onFailure(Throwable::printStackTrace)
        }
    }

    fun exportColl(coll: CompColl, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            kotlin.runCatching {
                val pubFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (pubFolder.exists() || pubFolder.mkdirs()) {
                    val file = pubFolder.resolve("${coll.name}.org.txt")
                    /*if (file.canWrite())*/ OrgCollExport.toStream(file.outputStream(), coll.id, context)
                }
            }.onFailure(Throwable::printStackTrace)
        }
    }
}

private fun CompColl.getGroupPkgs(context: Context, limit: Int = 3): Map<String, PackageInfo> {
    val pkgSize = groups.sumOf { it.apps.size.coerceAtMost(limit) }
    if (pkgSize <= 0) return emptyMap()

    val names = groups.flatMapTo(LinkedHashSet(pkgSize)) { it.apps.take(limit).map(OrgApp::pkg) }
    val pkgs = OrgPkgInfoProvider.getAll()
    return buildMap(pkgSize) {
        for (p in pkgs) {
            if (p.packageName in names) put(p.packageName, p)
            if (size >= pkgSize) break
        }
    }
}
