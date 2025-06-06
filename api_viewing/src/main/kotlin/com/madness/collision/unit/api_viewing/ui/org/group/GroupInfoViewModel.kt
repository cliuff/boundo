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

package com.madness.collision.unit.api_viewing.ui.org.group

import android.content.Context
import android.content.pm.PackageInfo
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madness.collision.unit.api_viewing.apps.AppPkgLabelProvider
import com.madness.collision.unit.api_viewing.apps.PackageLabelProvider
import com.madness.collision.unit.api_viewing.apps.PkgLabelProviderImpl
import com.madness.collision.unit.api_viewing.env.AppInfoOwner
import com.madness.collision.unit.api_viewing.env.EnvPackages
import com.madness.collision.unit.api_viewing.ui.org.OrgPkgInfoProvider
import com.madness.collision.unit.api_viewing.ui.org.OrgPkgLabelProvider
import io.cliuff.boundo.org.data.repo.GroupRepository
import io.cliuff.boundo.org.data.repo.OrgCollRepo
import io.cliuff.boundo.org.model.OrgApp
import io.cliuff.boundo.org.model.OrgGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GroupInfoUiState(
    val groupName: String,
    val selPkgs: Set<String>,
    val installedApps: List<PackageInfo>,
    val isLoading: Boolean,
    val isSubmitOk: Boolean,
    val appOwnerApps: List<PackageInfo>,
)

class GroupInfoViewModel(savedState: SavedStateHandle) : ViewModel() {
    private val mutUiState: MutableStateFlow<GroupInfoUiState>
    val uiState: StateFlow<GroupInfoUiState>
    private var labelProvider: PackageLabelProvider = PkgLabelProviderImpl()
    private var appOwners: List<AppInfoOwner> = emptyList()
    private var groupRepo: GroupRepository? = null
    /** Coll ID to add group in. */
    private var modCollId: Int = -1
    /** Group ID to modify. */
    private var modGroupId: Int = -1
    private var modGroup: OrgGroup? = null

    private var initJob: Job? = null

    init {
        val state = GroupInfoUiState(
            groupName = "",
            selPkgs = emptySet(),
            installedApps = emptyList(),
            isLoading = false,
            isSubmitOk = false,
            appOwnerApps = emptyList(),
        )
        mutUiState = MutableStateFlow(state)
        uiState = mutUiState.asStateFlow()
    }

    fun init(context: Context, modCollId: Int, modGroupId: Int, initGroup: OrgGroup?) {
        if (initJob != null) return
        if (modCollId > 0) this.modCollId = modCollId
        if (modGroupId > 0) this.modGroupId = modGroupId
        initJob = viewModelScope.launch(Dispatchers.IO) {
            mutUiState.update { it.copy(isLoading = true) }
            val groupRepo = groupRepo ?: OrgCollRepo.group(context).also { groupRepo = it }

            val pkgInfoProvider = OrgPkgInfoProvider
            val pkgLabelProvider = OrgPkgLabelProvider

            if (initGroup != null) {
                mutUiState.update { currValue ->
                    currValue.copy(
                        groupName = initGroup.name,
                        selPkgs = initGroup.apps.run { mapTo(HashSet(size), OrgApp::pkg) },
                    )
                }
            }

            var lastLabelProvider = pkgLabelProvider
            if (modGroupId > 0) groupRepo.getGroup(modGroupId).collect { modGroup ->
                this@GroupInfoViewModel.modGroup = modGroup
                modGroup ?: return@collect
                val modSelPkgs = modGroup.apps.run { mapTo(HashSet(size), OrgApp::pkg) }
                val labels = modGroup.apps.associate { it.pkg to it.label }
                val appLabelProvider = AppPkgLabelProvider(labels + lastLabelProvider.pkgLabels)
                labelProvider = appLabelProvider.also { lastLabelProvider = it }
                val pkgs = pkgInfoProvider.getAll()
                val sortedPkgs = pkgs.filter { it.packageName in modSelPkgs }
                    .sortedWith(compareBy(labelProvider.pkgComparator, PackageInfo::packageName))

                mutUiState.update {
                    it.copy(
                        groupName = modGroup.name,
                        selPkgs = modSelPkgs,
                        installedApps = sortedPkgs,
                        isLoading = false,
                    )
                }

                if (appOwners.isEmpty()) {  // run once
                val appStoreOwners = EnvPackages.getAppStoreOwners(context).also { appOwners = it }
                val ownerPkgs = appStoreOwners.run { mapTo(HashSet(size), AppInfoOwner::packageName) }
                    .let { pkgSet -> pkgs.filter { it.packageName in pkgSet } }
                appLabelProvider.retrieveLabels(ownerPkgs, context.packageManager)
                mutUiState.update { it.copy(appOwnerApps = ownerPkgs) }
                }
            }
        }
    }

    /** Label: non-empty label, or package name. */
    fun getPkgLabel(pkg: String): String {
        return labelProvider.getLabelOrPkg(pkg)
    }

    fun getAppOwner(pkg: String): AppInfoOwner? {
        return appOwners.find { it.packageName == pkg }
    }

    fun remove() {
        val groupRepo = groupRepo ?: return
        val group = modGroup ?: return
        val modCid = modCollId
        viewModelScope.launch(Dispatchers.IO) {
            if (modCid > 0) {
                val isOk = groupRepo.removeGroup(modCid, group)
                if (isOk) mutUiState.update { it.copy(isSubmitOk = true) }
            }
        }
    }
}