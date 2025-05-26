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
import com.madness.collision.unit.api_viewing.info.AppInfo
import com.madness.collision.unit.api_viewing.ui.upd.item.ApiUpdGuiArt
import com.madness.collision.unit.api_viewing.ui.upd.item.AppInstallVersion
import com.madness.collision.unit.api_viewing.ui.upd.item.GuiArt
import com.madness.collision.unit.api_viewing.ui.upd.item.GuiArtImpl
import com.madness.collision.unit.api_viewing.ui.upd.item.UpdGuiArt
import com.madness.collision.unit.api_viewing.ui.upd.item.VerUpdGuiArt

internal fun UpdatedApp.Upgrade.toGuiArt(context: Context, onClick: () -> Unit): ApiUpdGuiArt {
    val times = updateTime.toList().map { timestamp ->
        DateUtils.getRelativeTimeSpanString(
            timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
    }
    return ApiUpdGuiArt(
        art = app.toGuiArtImpl(context, onClick),
        oldApiInfo = VerInfo(targetApi.first),
        newApiInfo = VerInfo(targetApi.second),
        oldVersion = AppInstallVersion(versionCode.first, versionName.first, times[0]),
        newVersion = AppInstallVersion(versionCode.second, versionName.second, times[1]),
    )
}

internal fun UpdatedApp.VersionUpgrade.toGuiArt(context: Context, onClick: () -> Unit): VerUpdGuiArt {
    val times = updateTime.toList().map { timestamp ->
        DateUtils.getRelativeTimeSpanString(
            timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
    }
    return VerUpdGuiArt(
        art = app.toGuiArtImpl(context, onClick),
        apiInfo = VerInfo.targetDisplay(app),
        oldVersion = AppInstallVersion(versionCode.first, versionName.first, times[0]),
        newVersion = AppInstallVersion(versionCode.second, versionName.second, times[1]),
    )
}

internal fun ApiViewingApp.toGuiArt(context: Context, onClick: () -> Unit): UpdGuiArt {
    val app = this
    val time = DateUtils.getRelativeTimeSpanString(
        updateTime, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
    return UpdGuiArt(
        art = app.toGuiArtImpl(context, onClick),
        updateTime = time,
        apiInfo = VerInfo.targetDisplay(app),
    )
}

internal fun ApiViewingApp.toGuiArtImpl(context: Context, onClick: () -> Unit) =
    GuiArtImpl(
        identity = GuiArtIdentity(this, context),
        expTags = AppInfo.getExpTags(this, context),
        onClick = onClick,
    )

@Suppress("FunctionName")
internal fun GuiArtIdentity(app: ApiViewingApp, context: Context) =
    GuiArt.Identity(
        uid = app.appPackage.basePath,
        packageName = app.packageName,
        label = app.name,
        iconPkgInfo = AppPackageInfo(context, app),
    )

internal fun ApiUpdGuiArt.withUpdatedTags(app: ApiViewingApp, context: Context) =
    copy(art = GuiArtImpl(identity, AppInfo.getExpTags(app, context), onClick))

internal fun VerUpdGuiArt.withUpdatedTags(app: ApiViewingApp, context: Context) =
    copy(art = GuiArtImpl(identity, AppInfo.getExpTags(app, context), onClick))

internal fun UpdGuiArt.withUpdatedTags(app: ApiViewingApp, context: Context) =
    copy(art = GuiArtImpl(identity, AppInfo.getExpTags(app, context), onClick))
