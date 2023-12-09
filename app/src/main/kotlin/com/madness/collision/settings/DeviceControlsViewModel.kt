/*
 * Copyright 2023 Clifford Liu
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

package com.madness.collision.settings

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madness.collision.BuildConfig
import com.madness.collision.chief.chiefContext
import com.madness.collision.misc.PackageCompat
import com.madness.collision.util.os.OsUtils
import com.madness.collision.versatile.MyControlService
import com.madness.collision.versatile.controls.AtControlsProvider
import com.madness.collision.versatile.controls.MduControlsProvider
import com.madness.collision.versatile.ctrl.AudioTimerControlCreator
import com.madness.collision.versatile.ctrl.ControlInfo
import com.madness.collision.versatile.ctrl.DevManControlCreator
import com.madness.collision.versatile.ctrl.MduControlCreator
import com.madness.collision.versatile.ctrl.TimerValues
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DeviceControlsViewModel : ViewModel() {
    private val mutEnabledState: MutableState<Boolean> = mutableStateOf(false)
    val enabledState: State<Boolean> by ::mutEnabledState
    private val mutControlListState: MutableState<List<ControlInfo>> = mutableStateOf(emptyList())
    val controlListState: State<List<ControlInfo>> by ::mutControlListState
    private val initMutex = Mutex()

    fun init() {
        val context = chiefContext
        if (OsUtils.dissatisfy(OsUtils.R)) return
        viewModelScope.launch(Dispatchers.Default) {
            initMutex.withLock {
                mutEnabledState.value = isFeatureEnabled(context)
                if (controlListState.value.isEmpty()) {
                    mutControlListState.value = loadControls(context)
                }
            }
        }
    }

    fun setEnabled(isEnabled: Boolean) {
        val context = chiefContext
        setFeatureEnabled(context, isEnabled)
        mutEnabledState.value = isFeatureEnabled(context)
    }
}

@Suppress("deprecation")
private val flagGetDisabledLegacy = PackageManager.GET_DISABLED_COMPONENTS

private fun isFeatureEnabled(context: Context): Boolean {
    val pm = context.packageManager
    val compPkg = BuildConfig.APPLICATION_ID
    val ctrlService = MyControlService::class.java.name
    val compName = ComponentName(compPkg, ctrlService)
    when (pm.getComponentEnabledSetting(compName)) {
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> return true
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED -> return false
        PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> Unit
        else -> Unit
    }
    try {
        val flags = PackageManager.GET_SERVICES or when {
            OsUtils.satisfy(OsUtils.N) -> PackageManager.MATCH_DISABLED_COMPONENTS
            else -> flagGetDisabledLegacy
        }
        val pkg = PackageCompat.getInstalledPackage(pm, compPkg, flags)
        val service = pkg?.services?.find { it.name == ctrlService }
        return service?.isEnabled == true
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return false
}

private fun setFeatureEnabled(context: Context, isEnabled: Boolean) {
    val state = when {
        isEnabled -> PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        else -> PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    }
    val comp = ComponentName(BuildConfig.APPLICATION_ID, MyControlService::class.java.name)
    context.packageManager.setComponentEnabledSetting(comp, state, PackageManager.DONT_KILL_APP)
}

@RequiresApi(Build.VERSION_CODES.R)
private suspend fun loadControls(context: Context): List<ControlInfo> {
    val controlIds = buildMap {
        val atCreator = AudioTimerControlCreator(TimerValues(120f, 5f, 120f))
        put(atCreator, listOf(AtControlsProvider.DEV_ID_AT))
        put(MduControlCreator(), listOf(MduControlsProvider.DEV_ID_MDU))
        // skip DevManControlCreator.onCreate() call to avoid unnecessary receiver registration
        DevManControlCreator().let { put(it, it.getDeviceIds(context)) }
    }
    val controls = controlIds.flatMap { (creator, ids) ->
        ids.mapNotNull { id -> creator.create(context, id) }
    }
    // close creators
    for (creator in controlIds.keys) {
        when (creator) {
            is AudioTimerControlCreator, is MduControlCreator -> Unit
            is DevManControlCreator -> creator.close(context)
        }
    }
    return controls
}
