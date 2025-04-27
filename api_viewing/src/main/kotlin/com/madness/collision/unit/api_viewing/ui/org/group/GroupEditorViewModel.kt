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
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madness.collision.chief.app.SavedStateDelegate
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
import io.cliuff.boundo.org.data.usecase.CollUseCase
import io.cliuff.boundo.org.model.CollInfo
import io.cliuff.boundo.org.model.OrgApp
import io.cliuff.boundo.org.model.OrgGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    private val stateDelegate = SavedStateDelegate(savedState)
    private var selPkgList: ArrayList<String>? by stateDelegate
    var selCollId: Int? by stateDelegate
    var collName: String? by stateDelegate
    var groupName: String? by stateDelegate
    var selPkgs: Set<String>? = selPkgList?.toSet()

    init {
        savedState.setSavedStateProvider("GroupSavedStateProvider") {
            selPkgList = selPkgs?.let(::ArrayList)
            bundleOf()
        }
    }
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
    /**
     * The package names of selected apps from the group to modify,
     * or the maximum set of selected apps when creating a new group.
     */
    val modPkgs: Set<String>,
)

private sealed interface EditorModId {
    val isCreating: Boolean
        get() = this !is ModifyGroup

    /** Create new group in a new collection. */
    object CreateGroupNewColl : EditorModId

    /** Create new group in a selected existing collection. */
    class CreateGroupInColl(coll: Int) : _CollMod(coll)

    /** Modify group from an existing collection. */
    class ModifyGroup(coll: Int, val group: Int) : _CollMod(coll) {
        init { require(group > 0) }
    }

    sealed class _CollMod(val coll: Int) : EditorModId {
        init { require(coll > 0) }
    }
}

class GroupEditorViewModel(savedState: SavedStateHandle) : ViewModel() {
    private val mutUiState: MutableStateFlow<GroupUiState>
    val uiState: StateFlow<GroupUiState>
    private var labelProvider: PackageLabelProvider = PkgLabelProviderImpl()
    private var pkgPartitionMap: Map<String, String> = emptyMap()
    private var pkgGroupsMap: Map<String, List<OrgGroup>> = emptyMap()
    private var collRepo: CollRepository? = null
    private var groupRepo: GroupRepository? = null
    private var editId: EditorModId = EditorModId.CreateGroupNewColl
    private var submittedGroupId: Int = -1

    private val savedObj: GroupSavedState = GroupSavedState(savedState)
    private var initJob: Job? = null

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
            modPkgs = emptySet(),
        )
        mutUiState = MutableStateFlow(state)
        uiState = mutUiState.asStateFlow()
    }

    fun init(context: Context, modCollId: Int, modGroupId: Int) {
        if (initJob != null) return
        val modId = when {
            (modCollId > 0 && modGroupId > 0) -> EditorModId.ModifyGroup(modCollId, modGroupId)
            (modCollId > 0) -> EditorModId.CreateGroupInColl(modCollId)
            else -> EditorModId.CreateGroupNewColl
        }
        editId = modId

        initJob = viewModelScope.launch(Dispatchers.IO) {
            mutUiState.update { it.copy(isLoading = true) }
            val collRepo = collRepo ?: OrgCollRepo.coll(context).also { collRepo = it }
            val groupRepo = groupRepo ?: OrgCollRepo.group(context).also { groupRepo = it }

            labelProvider = OrgPkgLabelProvider
            val pkgInfoProvider = OrgPkgInfoProvider
            val pkgLabelProvider = OrgPkgLabelProvider
            val pkgMgr = context.packageManager
            MultiStageAppList.load(pkgInfoProvider, pkgMgr, pkgLabelProvider)
                // skip first stage when modifying group
                .runIf({ !modId.isCreating }, { drop(1) })
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

            pkgGroupsMap = if (modId is EditorModId._CollMod) groupRepo.getAppGroups(modId.coll) else emptyMap()
            val modGroup = if (modId is EditorModId.ModifyGroup) groupRepo.getOneOffGroup(modId.group) else null
            val modGroupName = modGroup?.name ?: ""
            val modSelPkgs = modGroup?.apps?.run { mapTo(HashSet(size), OrgApp::pkg) } ?: emptySet()
            // Put labels of selected apps, note this can override labels from retrieved app list.
            val modSelLabels = modGroup?.apps?.associate { it.pkg to it.label } ?: emptyMap()
            if (modSelLabels.isNotEmpty()) pkgLabelProvider.putLabels(modSelLabels)

            mutUiState.update { currValue ->
                currValue.copy(
                    groupName = savedObj.groupName ?: modGroupName,
                    selPkgs = savedObj.selPkgs ?: modSelPkgs,
                    // if modifying, use modSelPkgs; else (creating) use saved selPkgs
                    modPkgs = if (modGroup != null) modSelPkgs else savedObj.selPkgs.orEmpty(),
                )
            }

            // retrieve collections when creating group
            if (modId.isCreating) {
                collRepo.getCollections()
                    .onEach { colls ->
                        mutUiState.update { currValue ->
                            val coll = currValue.selColl ?: savedObj.selCollId
                                ?.let { cid -> colls.firstOrNull { it.id == cid } }
                            currValue.copy(
                                collName = coll?.name ?: "",
                                selColl = coll,
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
        editId = EditorModId.CreateGroupInColl(coll.id)
        savedObj.selCollId = coll.id
        mutUiState.update { it.copy(collName = coll.name, selColl = coll) }
    }

    fun setCollName(name: String) {
        // if input coll name matches selColl name, create in selColl
        val selColl = uiState.value.selColl
        editId = when (selColl?.name) {
            name -> EditorModId.CreateGroupInColl(selColl.id)
            else -> EditorModId.CreateGroupNewColl
        }
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
        // always add new selections to modPkgs when creating group
        val newMod = when {
            editId.isCreating && isSelected -> currentState.modPkgs + pkg
            else -> currentState.modPkgs
        }

        savedObj.selPkgs = newSel
        val newState = currentState.copy(selPkgs = newSel, modPkgs = newMod)
        mutUiState.update { newState }
    }

    fun submitEdits() {
        if (submittedGroupId > 0) return
        val collRepo = collRepo ?: return
        val groupRepo = groupRepo ?: return
        val state = uiState.value
        viewModelScope.launch(Dispatchers.IO) {
            val useCase = CollUseCase(collRepo, groupRepo, labelProvider::getLabel)
            state.run {
                submittedGroupId = when (val modId = editId) {
                    EditorModId.CreateGroupNewColl ->
                        useCase.createGroup(-1, collName, groupName, selPkgs).second
                    is EditorModId.CreateGroupInColl ->
                        useCase.createGroup(modId.coll, collName, groupName, selPkgs).second
                    is EditorModId.ModifyGroup ->
                        useCase.modifyGroup(modId.group, groupName, selPkgs)
                }
            }
            if (submittedGroupId > 0) {
                mutUiState.update { it.copy(isSubmitOk = true) }
            }
        }
    }
}