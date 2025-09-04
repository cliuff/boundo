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

package com.madness.collision.unit.api_viewing.ui.comp

import android.content.Context
import android.text.format.DateUtils
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.data.AppPackageInfo
import com.madness.collision.unit.api_viewing.data.VerInfo
import com.madness.collision.unit.api_viewing.ui.upd.item.GuiArt.Identity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlin.math.abs
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

object ArtMapper {
    /** Get time relative to [targetTime], refreshed periodically. */
    fun getRelativeTimeUpdates(targetTime: Long): Pair<String, Flow<String>?> {
        val freshTime = 1.hours.inWholeMilliseconds

        fun getRelativeTimeSpan(time: Long, now: Long): String {
            return DateUtils.getRelativeTimeSpanString(
                time, now, DateUtils.MINUTE_IN_MILLIS).toString()
        }

        val initTime = System.currentTimeMillis()
        val initResult = getRelativeTimeSpan(targetTime, initTime)
        if (abs(initTime - targetTime) >= freshTime) {
            return initResult to null
        }

        return initResult to flow {
            // delay some time to override the initial value
            if (System.currentTimeMillis() - initTime < 500) {
                delay(30.seconds)
            }
            while (true) {
                // delay at the end of an iteration,
                // emit update immediately when recollecting
                val now = System.currentTimeMillis()
                emit(getRelativeTimeSpan(targetTime, now))
                if (abs(now - targetTime) >= freshTime) break
                delay(45.seconds)
            }
        }
            .distinctUntilChanged()
    }
}

internal fun ApiViewingApp.toGuiArt(context: Context): GuiArtApp {
    val app = this
    return GuiArtApp(
        identity = GuiArtIdentity(app, context),
        updateTime = updateTime,
        compileApiInfo = VerInfo(app.compileAPI),
        targetApiInfo = VerInfo(app.targetAPI),
        minApiInfo = VerInfo(app.minAPI),
        isArchive = isArchive,
    )
}

@Suppress("FunctionName")
internal fun GuiArtIdentity(app: ApiViewingApp, context: Context) =
    Identity(
        uid = app.appPackage.basePath,
        packageName = app.packageName,
        label = app.name,
        iconPkgInfo = AppPackageInfo(context, app),
    )
