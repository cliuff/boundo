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
import com.madness.collision.unit.api_viewing.data.VerInfo
import com.madness.collision.unit.api_viewing.info.AppInfo
import com.madness.collision.unit.api_viewing.ui.upd.item.ApiUpdGuiArt
import com.madness.collision.unit.api_viewing.ui.upd.item.AppInstallVersion
import com.madness.collision.unit.api_viewing.ui.upd.item.UpdGuiArt
import com.madness.collision.unit.api_viewing.upgrade.Upgrade
import com.madness.collision.unit.api_viewing.upgrade.new

internal fun Upgrade.toGuiArt(context: Context, onClick: () -> Unit): ApiUpdGuiArt {
    val times = updateTime.toList().map { timestamp ->
        DateUtils.getRelativeTimeSpanString(
            timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
    }
    return ApiUpdGuiArt(
        packageName = new.packageName,
        label = new.name,
        iconPkgInfo = AppPackageInfo(context, new),
        expTags = AppInfo.getExpTags(new, context),
        onClick = onClick,
        oldApiInfo = VerInfo(targetApi.first),
        newApiInfo = VerInfo(targetApi.second),
        oldVersion = AppInstallVersion(versionCode.first, versionName.first, times[0]),
        newVersion = AppInstallVersion(versionCode.second, versionName.second, times[1]),
    )
}

internal fun ApiViewingApp.toGuiArt(context: Context, onClick: () -> Unit): UpdGuiArt {
    val app = this
    val time = DateUtils.getRelativeTimeSpanString(
        updateTime, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
    return UpdGuiArt(
        packageName = packageName,
        label = name,
        iconPkgInfo = AppPackageInfo(context, app),
        updateTime = time,
        apiInfo = VerInfo.targetDisplay(app),
        expTags = AppInfo.getExpTags(app, context),
        onClick = onClick,
    )
}

internal fun ApiUpdGuiArt.withUpdatedTags(app: ApiViewingApp, context: Context) =
    copy(expTags = AppInfo.getExpTags(app, context))

internal fun UpdGuiArt.withUpdatedTags(app: ApiViewingApp, context: Context) =
    copy(expTags = AppInfo.getExpTags(app, context))
