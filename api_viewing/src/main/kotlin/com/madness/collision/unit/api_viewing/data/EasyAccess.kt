/*
 * Copyright 2020 Clifford Liu
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

package com.madness.collision.unit.api_viewing.data

import android.content.Context
import android.content.SharedPreferences
import com.madness.collision.unit.api_viewing.AppTag
import com.madness.collision.unit.api_viewing.util.PrefUtil
import com.madness.collision.util.P

internal object EasyAccess {
    var isSweet: Boolean = false
    val shouldShowDesserts: Boolean
        get() = isSweet
    var shouldIncludeDisabled = false
    /**
     * indicate that in viewing target api mode
     */
    var isViewingTarget = false

    fun init(context: Context, prefSettings: SharedPreferences = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)) {
        load(context, prefSettings, false)
    }

    fun isChanged(context: Context, prefSettings: SharedPreferences = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)): Boolean {
        return load(context, prefSettings, true)
    }

    /**
     * @param isLazy whether to fast detect change. True to detect change, false to enforce complete loading.
     * @return whether there is change
     */
    fun load(context: Context, prefSettings: SharedPreferences = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE), isLazy: Boolean): Boolean {
        var isChanged = false
        isViewingTarget = prefSettings.getBoolean(PrefUtil.AV_VIEWING_TARGET, PrefUtil.AV_VIEWING_TARGET_DEFAULT).also {
            isChanged = isChanged || it != isViewingTarget
            if (isLazy && isChanged) return true
        }
        isSweet = prefSettings.getBoolean(PrefUtil.AV_SWEET, PrefUtil.AV_SWEET_DEFAULT).also {
            isChanged = isChanged || it != isSweet
            if (isLazy && isChanged) return true
        }
        shouldIncludeDisabled = prefSettings.getBoolean(PrefUtil.AV_INCLUDE_DISABLED, PrefUtil.AV_INCLUDE_DISABLED_DEFAULT).also {
            isChanged = isChanged || it != shouldIncludeDisabled
            if (isLazy && isChanged) return true
        }

        isChanged = AppTag.loadTagSettings(prefSettings, isLazy) || isChanged
        return isChanged
    }
}
