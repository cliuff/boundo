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

package com.madness.collision.instant

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import com.madness.collision.R
import com.madness.collision.main.ImmortalActivity
import com.madness.collision.main.MainActivity
import com.madness.collision.unit.Unit
import com.madness.collision.util.P
import com.madness.collision.util.SystemUtil
import com.madness.collision.util.X

class Instant(private val context: Context, private val manager: ShortcutManager? = null){
    val dynamicShortcuts: List<ShortcutInfo>
    @RequiresApi(Build.VERSION_CODES.N_MR1)
    get() = manager?.dynamicShortcuts ?: emptyList()

    private fun getShortcutBuildRes(id: String): ShortcutBuildRes? {
        val localeContext = SystemUtil.getLocaleContextSys(context)
        return when (id) {
            P.SC_ID_API_VIEWER -> ShortcutBuildRes(
                    Intent(Intent.ACTION_VIEW, Uri.EMPTY, context, MainActivity::class.java).putExtras(
                            MainActivity.forItem(Unit.UNIT_NAME_API_VIEWING)
                    ),
                    localeContext.getString(R.string.apiViewerShort),
                    localeContext.getString(R.string.apiViewer),
                    if (X.aboveOn(X.O)) R.mipmap.shortcut_api else R.drawable.shortcut_api_vector
            )
            P.SC_ID_AUDIO_TIMER -> ShortcutBuildRes(
                    Intent(Intent.ACTION_VIEW, Uri.EMPTY, context, MainActivity::class.java).putExtras(
                            MainActivity.forItem(Unit.UNIT_NAME_AUDIO_TIMER)
                    ),
                    localeContext.getString(R.string.unit_audio_timer),
                    localeContext.getString(R.string.unit_audio_timer),
                    if (X.aboveOn(X.O)) R.mipmap.ic_shortcut_audio_timer else R.drawable.ic_shortcut_audio_timer_legacy
            )
            P.SC_ID_IMMORTAL -> ShortcutBuildRes(
                    Intent(Intent.ACTION_VIEW, Uri.EMPTY, context, ImmortalActivity::class.java),
                    "Immortal", "Immortal", null
            )
            else -> null
        }
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    fun buildShortcut(id: String): ShortcutInfo {
        val buildRes = getShortcutBuildRes(id)
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

    fun buildLegacyShortcutIntent(id: String): Intent = Intent().apply {
        action = "com.android.launcher.action.INSTALL_SHORTCUT"
        // Avoid duplicate
        putExtra("duplicate", false)
        val buildRes = getShortcutBuildRes(id) ?: return@apply
        putExtra(legacyExtraShortcutIntent, buildRes.intent)
        putExtra(legacyExtraShortcutName, buildRes.shortLabel)
        if (buildRes.iconRes != null) {
            val iconRes = Intent.ShortcutIconResource.fromContext(context, buildRes.iconRes)
            putExtra(legacyExtraShortcutIcon, iconRes)
        }
    }

    @Suppress("deprecation")
    private val legacyExtraShortcutIntent = Intent.EXTRA_SHORTCUT_INTENT

    @Suppress("deprecation")
    private val legacyExtraShortcutName = Intent.EXTRA_SHORTCUT_NAME

    @Suppress("deprecation")
    private val legacyExtraShortcutIcon = Intent.EXTRA_SHORTCUT_ICON_RESOURCE

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    fun addDynamicShortcuts(vararg ids: String){
        if (ids.isEmpty()) return
        manager ?: return
        val shortcuts: MutableList<ShortcutInfo> = mutableListOf()
        ids.forEach { shortcuts.add(buildShortcut(it)) }
        manager.addDynamicShortcuts(shortcuts)
    }

    fun pinShortcut(id: String) {
        if (id.isEmpty()) return
        if (X.aboveOn(X.O) && manager != null && manager.isRequestPinShortcutSupported) {
            manager.requestPinShortcut(buildShortcut(id), null)
        } else {
            context.sendBroadcast(buildLegacyShortcutIntent(id))
        }
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    fun removeDynamicShortcuts(vararg ids: String){
        if (ids.isEmpty()) return
        manager ?: return
        manager.removeDynamicShortcuts(ids.toList())
    }

    /**
     * Rebuild all the dynamic shortcuts and update.
     */
    @RequiresApi(Build.VERSION_CODES.N_MR1)
    fun refreshAllDynamicShortcuts() = mutableListOf<ShortcutInfo>().run {
        manager ?: return@run
        dynamicShortcuts.forEach { add(buildShortcut(it.id)) }
        manager.updateShortcuts(this)
    }

    /**
     * Rebuild the dynamic shortcuts with given ids.
     * Useful when shortcut resources are updated,
     * in which case the affected shortcuts will behave unexpectedly in launcher.
     */
    @RequiresApi(Build.VERSION_CODES.N_MR1)
    fun refreshDynamicShortcuts(vararg ids: String) {
        manager ?: return
        val shortcuts = mutableListOf<ShortcutInfo>()
        // below: get all ids that can be found in the present dynamic shortcuts then rebuild them
        ids.toList().let { idList ->
            dynamicShortcuts.filter { idList.contains(it.id) }.map { it.id }
        }.forEach { shortcuts.add(buildShortcut(it)) }
        manager.updateShortcuts(shortcuts)
    }
}
