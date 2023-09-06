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

package com.madness.collision.unit.api_viewing.env

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import com.madness.collision.BuildConfig
import com.madness.collision.util.os.OsUtils

// EnvironmentPackages
object EnvPackages {
    val ActionShowAppInfo: String
        get() = if (OsUtils.satisfy(OsUtils.N)) Intent.ACTION_SHOW_APP_INFO
        else "android.intent.action.SHOW_APP_INFO"
    val ExtraPackageName: String
        get() = if (OsUtils.satisfy(OsUtils.N)) Intent.EXTRA_PACKAGE_NAME
        else "android.intent.extra.PACKAGE_NAME"

    @Suppress("deprecation")
    private fun PackageManager.queryIntentLegacy(intent: Intent) =
        queryIntentActivities(intent, 0)

    @Suppress("deprecation")
    private fun PackageManager.resolveActivityLegacy(intent: Intent, flags: Int) =
        resolveActivity(intent, flags)

    fun getDefaultSettings(context: Context): String? {
        val intent = Intent(Settings.ACTION_SETTINGS)
        val flag = PackageManager.MATCH_DEFAULT_ONLY
        val pm = context.packageManager
        val info = when {
            OsUtils.dissatisfy(OsUtils.T) -> pm.resolveActivityLegacy(intent, flag)
            else -> pm.resolveActivity(intent, PackageManager.ResolveInfoFlags.of(flag.toLong()))
        }
        return info?.activityInfo?.packageName
    }

    fun getStandardAppInfoOwners(context: Context): List<StandardAppInfoOwner> {
        val intent = Intent(ActionShowAppInfo)
        val pm = context.packageManager
        val activities = when {
            OsUtils.dissatisfy(OsUtils.T) -> pm.queryIntentLegacy(intent)
            else -> pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        }
        return activities.mapNotNull owner@{ activity ->
            val info = activity.activityInfo ?: return@owner null
            StandardAppInfoOwner(info.packageName, info.name)
        }
    }

    fun getInstalledAppInfoOwners(context: Context): List<AppInfoOwner> {
        val packageSettings = getDefaultSettings(context) ?: "com.android.settings"
        val standardOwners = getStandardAppInfoOwners(context)
        val map = HashMap<String, AppInfoOwner>(standardOwners.size + 4).apply {
            // Google Play Store itself is a standard owner, this serves as a fallback
            put(GooglePlayAppInfoOwner.packageName, GooglePlayAppInfoOwner)
            put(CoolApkAppInfoOwner.packageName, CoolApkAppInfoOwner)
            put(AppManagerAppInfoOwner.packageName, AppManagerAppInfoOwner)
            put(packageSettings, SettingsAppInfoOwner(packageSettings))
            // put standard owners afterwards to override custom owners, in reversed order
            // (in case one package / multiple activities, to retain the smaller-index one)
            putAll(standardOwners.asReversed().associateBy { it.packageName })
            // remove self owner (though N/A right now)
            remove(BuildConfig.APPLICATION_ID)
        }
        return map.values.toList()
    }
}
