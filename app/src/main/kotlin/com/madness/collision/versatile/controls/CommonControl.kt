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

package com.madness.collision.versatile.controls

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.controls.Control.StatelessBuilder
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toIcon
import com.madness.collision.R
import com.madness.collision.util.os.OsUtils
import com.madness.collision.versatile.ctrl.ControlInfo

inline fun <T> CommonControl(context: Context, block: CommonControlScope.() -> T): T {
    return block(CommonControlScope(context))
}

class CommonControlScope(private val context: Context) {
    fun activityIntent(intent: Intent): PendingIntent {
        val intentFlags = when {
            OsUtils.satisfy(OsUtils.M) -> PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            else -> PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getActivity(context, 0, intent, intentFlags)
    }

    fun drawableIcon(@DrawableRes id: Int): Icon? {
        // Icon.setTint() does not work for control adding page
        // Icon.createWithResource(context, id).setTint(tint)
        return ContextCompat.getDrawable(context, id)?.run {
            setTint(ContextCompat.getColor(context, R.color.primaryAWhite))
            toBitmap().toIcon()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun StatelessBuilder.applyInfo(info: ControlInfo): StatelessBuilder {
        setTitle(info.title)
        setSubtitle(info.subtitle)
        setCustomIcon(info.icon)
        return this
    }
}