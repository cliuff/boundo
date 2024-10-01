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
import com.madness.collision.unit.api_viewing.data.EasyAccess
import com.madness.collision.unit.api_viewing.ui.list.AppApiMode
import com.madness.collision.util.P

typealias AppUpdatesOptions = AppApiMode

class AppUpdatesOptionsOwner {
    private var prefs: SharedPreferences? = null

    fun getOptions(context: Context): AppUpdatesOptions {
        val prefs = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE).also { prefs = it }
        EasyAccess.init(context, prefs)
        return if (EasyAccess.isViewingTarget) AppApiMode.Target else AppApiMode.Minimum
    }

    fun checkPrefsChanged(context: Context): Boolean {
        val p = prefs ?: return false
        return EasyAccess.load(context, p, false)
    }
}
