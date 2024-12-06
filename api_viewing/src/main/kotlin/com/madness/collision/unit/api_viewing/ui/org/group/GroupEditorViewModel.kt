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
import com.madness.collision.unit.api_viewing.apps.PlatformAppProvider
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Process death: editGroupName, selectedPkgs, submittedGroupId, scrollPos.
class GroupSavedState(savedState: SavedStateHandle) {
    private var selPkgList: ArrayList<String>? by savedState.prop()
    var groupName: String? by savedState.prop()
    var selPkgs: Set<String>?
        get() = selPkgList?.toSet()
        set(value) = ::selPkgList.set(value?.let(::ArrayList))
}

data class GroupUiState(
    val groupName: String,
    val selPkgs: Set<String>,
    val installedApps: List<PackageInfo>,
    val isLoading: Boolean,
)

class GroupEditorViewModel(savedState: SavedStateHandle) : ViewModel() {
    private val mutUiState: MutableStateFlow<GroupUiState>
    val uiState: StateFlow<GroupUiState>
    /** App label: non-empty or null. */
    private var pkgLabelMap: Map<String, String?> = emptyMap()
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
            isLoading = false,
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

            val pkgMgr = context.packageManager
            val pkgs = PlatformAppProvider(context).getAll()
            // load app labels, and sort app list by label
            val labels: Map<String, String?> = pkgs.associate { p ->
                val label = p.applicationInfo?.loadLabel(pkgMgr)?.toString()
                p.packageName to label?.takeUnless { it.isEmpty() }
            }
            pkgLabelMap = labels
            val sortedPkgs = pkgs.sortedBy { labels[it.packageName] ?: it.packageName }

            savedObj.groupName = "Unnamed Group"
            mutUiState.update {
                it.copy(groupName = "Unnamed Group", installedApps = sortedPkgs, isLoading = false)
            }
        }
    }

    /** Label: non-empty label, or package name. */
    fun getPkgLabel(pkg: String): String {
        return pkgLabelMap[pkg] ?: pkg
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
            val collId = if (modCid <= 0) {
                val createColl = CollInfo(0, "Unnamed Coll", 0)
                collRepo.addCollection(createColl)
            } else {
                modCid
            }
            if (modGid <= 0) {
                if (collId > 0) {
                    val updGroup = OrgGroup(0, state.groupName, state.selPkgs.map(::OrgApp))
                    val gid = groupRepo.addGroupAndApps(collId, updGroup)
                    if (gid > 0) submittedGroupId = gid
                }
            } else {
                val updGroup = OrgGroup(modGid, state.groupName, state.selPkgs.map(::OrgApp))
                groupRepo.updateGroupAndApps(updGroup)
                submittedGroupId = modGid
            }
        }
    }
}