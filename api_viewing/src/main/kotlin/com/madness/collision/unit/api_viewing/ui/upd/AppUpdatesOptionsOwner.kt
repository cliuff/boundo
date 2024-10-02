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

package com.madness.collision.unit.api_viewing.ui.upd

import android.content.Context
import android.content.SharedPreferences
import com.madness.collision.unit.api_viewing.AppTag
import com.madness.collision.unit.api_viewing.TriStateSelectable
import com.madness.collision.unit.api_viewing.data.EasyAccess
import com.madness.collision.unit.api_viewing.ui.list.AppApiMode
import com.madness.collision.unit.api_viewing.util.PrefUtil
import com.madness.collision.util.P

typealias AppUpdatesOptions = AppApiMode

class AppUpdatesOptionsOwner {
    private var prefs: SharedPreferences? = null
    private var lastApiMode: AppApiMode = AppApiMode.Target
    private var lastTagStates: Map<String, TriStateSelectable> = emptyMap()

    fun getOptions(context: Context): AppUpdatesOptions {
        val prefs = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE).also { prefs = it }
        loadTagSettings(prefs)
        return getApiMode(prefs).also { EasyAccess.isViewingTarget = it != AppApiMode.Minimum }
    }

    private fun getApiMode(prefs: SharedPreferences): AppApiMode {
        val isTargetApi = prefs.getBoolean(PrefUtil.AV_VIEWING_TARGET, PrefUtil.AV_VIEWING_TARGET_DEFAULT)
        val apiMode = if (isTargetApi) AppApiMode.Target else AppApiMode.Minimum
        return apiMode.also { lastApiMode = it }
    }

    private fun loadTagSettings(prefs: SharedPreferences): Map<String, TriStateSelectable> {
        AppTag.loadTagSettings(prefs, false)
        return AppTag.getTagSettings().also { lastTagStates = it }
    }

    fun checkPrefsChanged(context: Context): Boolean {
        val p = prefs ?: return false
        val isApiModeChanged = lastApiMode != getApiMode(p)
        return lastTagStates != loadTagSettings(p) || isApiModeChanged
    }
}
