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
import android.text.format.DateUtils
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.data.AppPackageInfo
import com.madness.collision.unit.api_viewing.data.UpdatedApp
import com.madness.collision.unit.api_viewing.data.VerInfo
import com.madness.collision.unit.api_viewing.ui.upd.item.AppInstallVersion
import com.madness.collision.unit.api_viewing.ui.upd.item.GuiArt

internal fun UpdatedApp.toGuiArt(context: Context): GuiArt =
    when (this) {
        is UpdatedApp.General -> app.toGuiArt(context)
        is UpdatedApp.Upgrade -> toGuiArt(context)
        is UpdatedApp.VersionUpgrade -> toGuiArt(context)
    }

private fun UpdatedApp.Upgrade.toGuiArt(context: Context): GuiArt.ApiUpdate {
    val apis = targetApi.toList().map(::VerInfo)
    val verArt = (this as UpdatedApp.VersionUpgrade).toGuiArt(context)
    return verArt.run { GuiArt.ApiUpdate(identity, apis[0], apis[1], oldVersion, newVersion) }
}

private fun UpdatedApp.VersionUpgrade.toGuiArt(context: Context): GuiArt.VerUpdate {
    val timeNow = System.currentTimeMillis()
    val times = updateTime.toList().map { timestamp ->
        DateUtils.getRelativeTimeSpanString(
            timestamp, timeNow, DateUtils.MINUTE_IN_MILLIS).toString()
    }
    return GuiArt.VerUpdate(
        identity = GuiArtIdentity(app, context),
        apiInfo = VerInfo(app.targetAPI),
        oldVersion = AppInstallVersion(versionCode.first, versionName.first, times[0]),
        newVersion = AppInstallVersion(versionCode.second, versionName.second, times[1]),
    )
}

internal fun ApiViewingApp.toGuiArt(context: Context): GuiArt.App {
    val app = this
    return GuiArt.App(
        identity = GuiArtIdentity(app, context),
        updateTime = updateTime,
        apiInfo = VerInfo(app.targetAPI),
    )
}

@Suppress("FunctionName")
internal fun GuiArtIdentity(app: ApiViewingApp, context: Context) =
    GuiArt.Identity(
        uid = app.appPackage.basePath,
        packageName = app.packageName,
        label = app.name,
        iconPkgInfo = AppPackageInfo(context, app),
    )
