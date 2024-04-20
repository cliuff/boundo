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

package com.madness.collision.unit.api_viewing.ui.list

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.madness.collision.unit.api_viewing.AppTag
import com.madness.collision.unit.api_viewing.TriStateSelectable
import com.madness.collision.unit.api_viewing.data.EasyAccess
import com.madness.collision.unit.api_viewing.util.PrefUtil
import com.madness.collision.util.P

class AppListOptionsOwner {
    private var prefs: SharedPreferences? = null

    fun getOptions(context: Context): AppListOptions {
        val prefs = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE).also { prefs = it }
        val sortItem = prefs.getInt(PrefUtil.AV_SORT_ITEM, AppListOrder.UpdateTime.code)
        val displayItem = prefs.getInt(PrefUtil.AV_LIST_SRC_ITEM, 0)
        val savedSrcSet = when (displayItem) {
            0 -> listOf(AppListSrc.UserApps)
            1 -> listOf(AppListSrc.SystemApps)
            2 -> listOf(AppListSrc.SystemApps, AppListSrc.UserApps)
            3 -> listOf(AppListSrc.DeviceApks)
            else -> listOf(AppListSrc.SystemApps, AppListSrc.UserApps)
        }
        EasyAccess.init(context, prefs)
        val apiMode = if (EasyAccess.isViewingTarget) AppApiMode.Target else AppApiMode.Minimum
        return AppListOptions(savedSrcSet, AppListOrderOrDefault(sortItem), apiMode)
    }

    fun setListSrc(changedSrc: AppListSrc, newSrcSet: Set<AppListSrc>) {
        when (changedSrc.cat) {
            ListSrcCat.Platform -> {
                val platformSrcSet = newSrcSet.filter { it.cat == ListSrcCat.Platform }
                val srcCode = when {
                    // todo at least one src required
                    platformSrcSet.isEmpty() -> 2
                    platformSrcSet.size >= 2 -> 2
                    platformSrcSet.firstOrNull() is AppListSrc.SystemApps -> 1
                    platformSrcSet.firstOrNull() is AppListSrc.UserApps -> 0
                    else -> 2
                }
                prefs?.edit { putInt(PrefUtil.AV_LIST_SRC_ITEM, srcCode) }
            }
            ListSrcCat.Storage -> Unit
            ListSrcCat.Temporary -> Unit
            ListSrcCat.Filter -> Unit
        }
    }

    fun setListOrder(order: AppListOrder) {
        prefs?.edit { putInt(PrefUtil.AV_SORT_ITEM, order.code) }
    }

    fun setApiMode(apiMode: AppApiMode) {
        EasyAccess.isViewingTarget = when (apiMode) {
            AppApiMode.Compile, AppApiMode.Target -> true
            AppApiMode.Minimum -> false
        }
        prefs?.edit { putBoolean(PrefUtil.AV_VIEWING_TARGET, EasyAccess.isViewingTarget) }
    }

    fun reloadTagSettings(checkedTags: Map<String, Boolean>? = null) {
        if (checkedTags != null) {
            val settings = checkedTags.mapValues { en -> TriStateSelectable(en.key, en.value) }
            AppTag.loadTagSettings(settings, false)
        } else {
            prefs?.let { AppTag.loadTagSettings(it, false) }
        }
    }
}