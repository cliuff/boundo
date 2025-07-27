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

internal fun ApiViewingApp.toGuiArt(context: Context): GuiArtApp {
    val app = this
    val time = DateUtils.getRelativeTimeSpanString(
        updateTime, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
    return GuiArtApp(
        identity = GuiArtIdentity(app, context),
        updateTime = time,
        compileApiInfo = VerInfo(app.compileAPI),
        targetApiInfo = VerInfo(app.targetAPI),
        minApiInfo = VerInfo(app.minAPI),
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
