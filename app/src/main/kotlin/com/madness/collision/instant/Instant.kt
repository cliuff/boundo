/*
 * Copyright 2021 Clifford Liu
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

package com.madness.collision.instant

import android.content.Context
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.madness.collision.chief.auth.AppOpsMaster
import com.madness.collision.util.os.OsUtils

@RequiresApi(OsUtils.N_MR1)
class Instant(private val context: Context, private val manager: ShortcutManager? = null) {

    val dynamicShortcuts: List<ShortcutInfo>
    get() = manager?.dynamicShortcuts ?: emptyList()

    fun buildShortcut(id: String): ShortcutInfo {
        val buildRes = InstantCompat.getShortcutBuildRes(context, id)
        return ShortcutInfo.Builder(context, id).run {
            buildRes ?: return build()
            setShortLabel(buildRes.shortLabel)
            setLongLabel(buildRes.longLabel)
            setIntent(buildRes.intent)
            if (buildRes.iconRes != null) {
                setIcon(Icon.createWithResource(context, buildRes.iconRes))
            }
            build()
        }
    }

    fun addDynamicShortcuts(vararg ids: String){
        if (ids.isEmpty()) return
        manager ?: return
        val shortcuts = ids.map { buildShortcut(it) } // small, few
        manager.addDynamicShortcuts(shortcuts)
    }

    fun pinShortcut(id: String) {
        if (id.isEmpty()) return
        if (AppOpsMaster.isShortcutPinAllowed().not()) {
            Toast.makeText(context, "Shortcut permission denied", Toast.LENGTH_SHORT).show()
            return
        }
        if (OsUtils.satisfy(OsUtils.O) && manager != null && manager.isRequestPinShortcutSupported) {
            manager.requestPinShortcut(buildShortcut(id), null)
        } else {
            InstantCompat.pinShortcutLegacy(context, id)
        }
    }

    fun removeDynamicShortcuts(vararg ids: String){
        if (ids.isEmpty()) return
        manager ?: return
        manager.removeDynamicShortcuts(ids.toList())
    }

    /**
     * Rebuild all the dynamic shortcuts and update.
     */
    fun refreshAllDynamicShortcuts() {
        manager ?: return
        val shortcuts = dynamicShortcuts.map { buildShortcut(it.id) } // small, few
        manager.updateShortcuts(shortcuts)
    }

    /**
     * Rebuild the dynamic shortcuts with given ids.
     * Useful when shortcut resources are updated,
     * in which case the affected shortcuts will behave unexpectedly in launcher.
     */
    fun refreshDynamicShortcuts(vararg ids: String) {
        manager ?: return
        // below: get all ids that can be found in the present dynamic shortcuts then rebuild them
        val shortcuts = dynamicShortcuts // small
            .map { it.id }
            .intersect(ids.toList()) // stateful, small
            .map { buildShortcut(it) } // few
        manager.updateShortcuts(shortcuts)
    }
}
