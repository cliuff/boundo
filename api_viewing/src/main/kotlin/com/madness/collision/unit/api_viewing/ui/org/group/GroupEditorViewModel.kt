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
import com.madness.collision.chief.app.prop
import com.madness.collision.chief.lang.runIf
import com.madness.collision.unit.api_viewing.apps.MultiStageAppList
import com.madness.collision.unit.api_viewing.apps.PackageLabelProvider
import com.madness.collision.unit.api_viewing.apps.PkgLabelProviderImpl
import com.madness.collision.unit.api_viewing.info.PartitionInfo
import com.madness.collision.unit.api_viewing.ui.org.OrgPkgInfoProvider
import com.madness.collision.unit.api_viewing.ui.org.OrgPkgLabelProvider
import io.cliuff.boundo.org.data.repo.CollRepository
import io.cliuff.boundo.org.data.repo.GroupRepository
import io.cliuff.boundo.org.data.repo.OrgCollRepo
import io.cliuff.boundo.org.model.CollInfo
import io.cliuff.boundo.org.model.OrgApp
import io.cliuff.boundo.org.model.OrgGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Process death: editGroupName, selectedPkgs, submittedGroupId, scrollPos.
class GroupSavedState(savedState: SavedStateHandle) {
    private var selPkgList: ArrayList<String>? by savedState.prop()
    var selCollId: Int? by savedState.prop()
    var collName: String? by savedState.prop()
    var groupName: String? by savedState.prop()
    var selPkgs: Set<String>?
        get() = selPkgList?.toSet()
        set(value) = ::selPkgList.set(value?.let(::ArrayList))
}

data class GroupUiState(
    val groupName: String,
    val selPkgs: Set<String>,
    val installedApps: List<PackageInfo>,
    /** A list of the end (exclusive) indices of grouping. Index <= 0 indicates an empty group. */
    val installedAppsGrouping: List<Int>,
    val isLoading: Boolean,
    val isSubmitOk: Boolean,
    /** Input or selected collection name. */
    val collName: String,
    val selColl: CollInfo?,
    val collList: List<CollInfo>,
)

class GroupEditorViewModel(savedState: SavedStateHandle) : ViewModel() {
    private val mutUiState: MutableStateFlow<GroupUiState>
    val uiState: StateFlow<GroupUiState>
    private var labelProvider: PackageLabelProvider = PkgLabelProviderImpl()
    private var pkgPartitionMap: Map<String, String> = emptyMap()
    private var pkgGroupsMap: Map<String, List<OrgGroup>> = emptyMap()
    private var collRepo: CollRepository? = null
    private var groupRepo: GroupRepository? = null
    /** Coll ID to add group in. */
    private var modCollId: Int = -1
    /** Group ID to modify. */
    private var modGroupId: Int = -1
    private var submittedGroupId: Int = -1

    private val savedObj: GroupSavedState = GroupSavedState(savedState)

    init {
        val state = GroupUiState(
            groupName = savedObj.groupName ?: "",
            selPkgs = savedObj.selPkgs ?: emptySet(),
            installedApps = emptyList(),
            installedAppsGrouping = emptyList(),
            isLoading = false,
            isSubmitOk = false,
            collName = savedObj.collName ?: "",
            selColl = null,
            collList = emptyList(),
        )
        mutUiState = MutableStateFlow(state)
        uiState = mutUiState.asStateFlow()
    }

    fun init(context: Context, modCollId: Int, modGroupId: Int) {
        if (modCollId > 0) this.modCollId = modCollId
        if (modGroupId > 0) this.modGroupId = modGroupId
        viewModelScope.launch(Dispatchers.IO) {
            mutUiState.update { it.copy(isLoading = true) }
            val collRepo = collRepo ?: OrgCollRepo.coll(context).also { collRepo = it }
            val groupRepo = groupRepo ?: OrgCollRepo.group(context).also { groupRepo = it }

            labelProvider = OrgPkgLabelProvider
            val pkgInfoProvider = OrgPkgInfoProvider
            val pkgLabelProvider = OrgPkgLabelProvider
            val pkgMgr = context.packageManager
            MultiStageAppList.load(pkgInfoProvider, pkgMgr, pkgLabelProvider)
                // skip first stage when modifying group
                .runIf({ modGroupId > 0 }, { drop(1) })
                .onEach { (pkgList, grouping) ->
                    pkgPartitionMap = PartitionInfo.getPkgPartitions(pkgList)
                    mutUiState.update {
                        it.copy(
                            installedApps = pkgList,
                            installedAppsGrouping = grouping,
                            isLoading = false,
                        )
                    }
                }
                .launchIn(this)

            pkgGroupsMap = if (modCollId > 0) groupRepo.getAppGroups(modCollId) else emptyMap()
            val modGroup = if (modGroupId > 0) groupRepo.getOneOffGroup(modGroupId) else null
            val modGroupName = modGroup?.name ?: ""
            val modSelPkgs = modGroup?.apps?.run { mapTo(HashSet(size), OrgApp::pkg) } ?: emptySet()

            savedObj.groupName = modGroupName
            savedObj.selPkgs = modSelPkgs
            mutUiState.update {
                it.copy(
                    groupName = modGroupName,
                    selPkgs = modSelPkgs,
                )
            }

            // retrieve collections when creating group
            if (modGroupId <= 0) {
                collRepo.getCollections()
                    .onEach { colls ->
                        mutUiState.update { currValue ->
                            currValue.copy(
                                selColl = currValue.selColl ?: savedObj.selCollId
                                    ?.let { cid -> colls.firstOrNull { it.id == cid } },
                                collList = colls,
                            )
                        }
                    }
                    .launchIn(this)
            }
        }
    }

    /** Label: non-empty label, or package name. */
    fun getPkgLabel(pkg: String): String {
        return labelProvider.getLabelOrPkg(pkg)
    }

    fun getPkgPartition(pkg: String): String? {
        return pkgPartitionMap[pkg]
    }

    fun getPkgGroups(pkg: String): List<OrgGroup> {
        return pkgGroupsMap[pkg].orEmpty()
    }

    fun selectColl(coll: CollInfo) {
        savedObj.selCollId = coll.id
        mutUiState.update { it.copy(collName = coll.name, selColl = coll) }
    }

    fun setCollName(name: String) {
        savedObj.collName = name
        mutUiState.update { it.copy(collName = name) }
    }

    fun setGroupName(name: String) {
        savedObj.groupName = name
        mutUiState.update { it.copy(groupName = name) }
    }

    fun setPkgSelected(pkg: String, isSelected: Boolean) {
        val currentState = uiState.value
        val selPkgSet = currentState.selPkgs
        val hasSelPkg = pkg in selPkgSet
        val newSel = when {
            isSelected && !hasSelPkg -> selPkgSet + pkg
            !isSelected && hasSelPkg -> selPkgSet - pkg
            else -> return
        }
        savedObj.selPkgs = newSel
        val newState = currentState.copy(selPkgs = newSel)
        mutUiState.update { newState }
    }

    fun submitEdits() {
        if (submittedGroupId > 0) return
        val collRepo = collRepo ?: return
        val groupRepo = groupRepo ?: return
        val state = uiState.value
        val modCid = modCollId
        val modGid = modGroupId
        viewModelScope.launch(Dispatchers.IO) {
            val time = System.currentTimeMillis()
            val isMod = modCid > 0 && state.run { selColl?.id == modCid && selColl.name == collName }
            val collName = state.collName.trim().takeUnless { it.isBlank() } ?: "Unnamed Coll"
            val collId = if (modGid <= 0 && !isMod) {
                val createColl = CollInfo(0, collName, time, time, 0)
                collRepo.addCollection(createColl)
            } else {
                modCid
            }
            val groupName = state.groupName.trim().takeUnless { it.isBlank() } ?: "Unnamed Group"
            val apps = state.selPkgs.map { pkg ->
                OrgApp(pkg, labelProvider.getLabel(pkg) ?: "", "", time, time)
            }
            if (modGid <= 0) {
                if (collId > 0) {
                    val updGroup = OrgGroup(0, groupName, time, time, apps)
                    val gid = groupRepo.addGroupAndApps(collId, updGroup)
                    if (gid > 0) submittedGroupId = gid
                }
            } else {
                val updGroup = OrgGroup(modGid, groupName, time, time, apps)
                groupRepo.updateGroupAndApps(updGroup)
                submittedGroupId = modGid
            }
            if (submittedGroupId > 0) {
                mutUiState.update { it.copy(isSubmitOk = true) }
            }
        }
    }
}