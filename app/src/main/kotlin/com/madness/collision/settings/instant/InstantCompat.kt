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

package com.madness.collision.settings.instant

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.madness.collision.R
import com.madness.collision.main.ImmortalActivity
import com.madness.collision.main.MainActivity
import com.madness.collision.unit.Unit
import com.madness.collision.util.P
import com.madness.collision.util.SystemUtil
import com.madness.collision.util.X

class ShortcutBuildRes(val intent: Intent, val shortLabel: String, val longLabel: String, val iconRes: Int?)

object InstantCompat {
    fun getShortcutBuildRes(context: Context, id: String): ShortcutBuildRes? {
        val localeContext = SystemUtil.getLocaleContextSys(context)
        return when (id) {
            P.SC_ID_AUDIO_TIMER -> ShortcutBuildRes(
                    Intent(Intent.ACTION_VIEW, Uri.EMPTY, context, MainActivity::class.java).putExtras(
                            MainActivity.forItem(Unit.UNIT_NAME_AUDIO_TIMER)
                    ).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    },
                    localeContext.getString(R.string.unit_audio_timer),
                    localeContext.getString(R.string.unit_audio_timer),
                    if (X.aboveOn(X.O)) R.mipmap.ic_shortcut_audio_timer else R.drawable.ic_shortcut_audio_timer_legacy
            )
            P.SC_ID_IMMORTAL -> ShortcutBuildRes(
                    Intent(Intent.ACTION_VIEW, Uri.EMPTY, context, ImmortalActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    },
                    "Immortal", "Immortal", null
            )
            else -> null
        }
    }

    fun buildLegacyShortcutIntent(context: Context, id: String): Intent = Intent().apply {
        action = "com.android.launcher.action.INSTALL_SHORTCUT"
        // Avoid duplicate
        putExtra("duplicate", false)
        val buildRes = getShortcutBuildRes(context, id) ?: return@apply
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

    fun pinShortcutLegacy(context: Context, id: String) {
        if (id.isEmpty()) return
        context.sendBroadcast(buildLegacyShortcutIntent(context, id))
        X.toast(context, R.string.text_done, Toast.LENGTH_SHORT)
    }
}
