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

package com.madness.collision.unit.api_viewing.ui.pref

import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.madness.collision.util.os.OsUtils
import com.madness.collision.versatile.TextProcessingActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ArtPrefsViewModel(app: Application) : AndroidViewModel(app) {
    private val mutUiState: MutableStateFlow<ArtPrefsUiState>
    val uiState: StateFlow<ArtPrefsUiState>

    private var initJob: Job? = null

    init {
        val state = ArtPrefsUiState(isTextProcessingEnabled = false)
        mutUiState = MutableStateFlow(state)
        uiState = mutUiState.asStateFlow()
    }

    fun init() {
        if (initJob != null) return
        initJob = viewModelScope.launch(Dispatchers.Default) {
            mutUiState.update { loadState() }
        }
    }

    private fun loadState(): ArtPrefsUiState {
        if (!OsUtils.satisfy(OsUtils.M)) {
            return ArtPrefsUiState(isTextProcessingEnabled = false)
        }
        val context = application
        val comp = ComponentName(context, TextProcessingActivity::class.java)
        val enabled = isActivityCompEnabled(context.packageManager, comp)
        return ArtPrefsUiState(isTextProcessingEnabled = enabled)
    }

    fun setTextProcessingEnabled(enabled: Boolean) {
        mutUiState.update { it.copy(isTextProcessingEnabled = enabled) }

        viewModelScope.launch(Dispatchers.Default) {
            val context = application
            val comp = ComponentName(context, TextProcessingActivity::class.java)
            setCompEnabled(context.packageManager, comp, enabled)
        }
    }
}

@Suppress("deprecation")
private val GetDisabledCompLegacy = PackageManager.GET_DISABLED_COMPONENTS

private fun isActivityCompEnabled(pkgMgr: PackageManager, comp: ComponentName): Boolean {
    when (pkgMgr.getComponentEnabledSetting(comp)) {
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> return true
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED -> return false
        PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> Unit
        else -> Unit
    }
    try {
        val info = pkgMgr.getActivityInfo(comp, when {
            OsUtils.satisfy(OsUtils.N) -> PackageManager.MATCH_DISABLED_COMPONENTS
            else -> GetDisabledCompLegacy
        })
        return info.applicationInfo.enabled && info.enabled
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
        return false
    }
}

private fun setCompEnabled(pkgMgr: PackageManager, comp: ComponentName, enabled: Boolean) {
    val state = when {
        enabled -> PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        else -> PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    }
    pkgMgr.setComponentEnabledSetting(comp, state, PackageManager.DONT_KILL_APP)
}
