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
import android.net.Uri
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
            // filter out disabled components
            if (!info.enabled) return@owner null
            StandardAppInfoOwner(info.packageName, info.name)
        }
    }

    fun getMarketAppInfoOwners(context: Context): List<MarketAppInfoOwner> {
        val intent = Intent(Intent.ACTION_VIEW)
            .addCategory(Intent.CATEGORY_DEFAULT)
            .setData(Uri.parse("market://details"))
        val pm = context.packageManager
        val activities = when {
            OsUtils.dissatisfy(OsUtils.T) -> pm.queryIntentLegacy(intent)
            else -> pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        }
        return activities.mapNotNull owner@{ activity ->
            val info = activity.activityInfo ?: return@owner null
            // filter out disabled components
            if (!info.enabled) return@owner null
            MarketAppInfoOwner(info.packageName, info.name)
        }
    }

    fun getInstalledAppInfoOwners(context: Context): List<AppInfoOwner> {
        val packageSettings = getDefaultSettings(context) ?: "com.android.settings"
        val standardOwners = getStandardAppInfoOwners(context)
        val marketOwners = getMarketAppInfoOwners(context)
        val map = HashMap<String, AppInfoOwner>(standardOwners.size + marketOwners.size + 5).apply {
            // Google Play Store itself is a standard owner, this serves as a fallback
            put(GooglePlayAppInfoOwner.packageName, GooglePlayAppInfoOwner)
            put(AppManagerAppInfoOwner.packageName, AppManagerAppInfoOwner)
            put(packageSettings, SettingsAppInfoOwner(packageSettings))
            // put standard owners afterwards to override custom owners, in reversed order
            // (in case one package / multiple activities, to retain the smaller-index one)
            putAll(marketOwners.asReversed().associateBy { it.packageName })
            putAll(standardOwners.asReversed().associateBy { it.packageName })
            // Xiaomi app store itself is a market owner that blocks app details intent,
            // use this custom owner instead as a workaround
            put(XiaomiAppInfoOwner.packageName, XiaomiAppInfoOwner)
            // remove self owner (though N/A right now)
            remove(BuildConfig.APPLICATION_ID)
            // remove unqualified owners
            for ((invalidPkg, invalidClassName) in UnqualifiedAppInfoOwners) {
                val addedOwner = get(invalidPkg) as? CompAppInfoOwner ?: continue
                if (addedOwner.comp.className == invalidClassName) remove(invalidPkg)
            }
            // remove disabled or uninstalled owners
            keys.retainAll { pkg ->
                // check activity existence for custom AppInfoOwners
                (get(pkg) as? GooglePlayAppInfoOwner)?.let { addedOwner ->
                    val intent = Intent().setComponent(addedOwner.comp)
                    val info = context.packageManager.resolveActivity(intent, 0)
                    return@retainAll info?.activityInfo?.enabled == true
                }
                kotlin.runCatching { context.packageManager.getApplicationInfo(pkg, 0) }
                    .fold({ it.enabled }, { false })
            }
        }
        return map.values.toList()
    }

    fun getAppStoreOwners(context: Context): List<AppInfoOwner> {
        val standardOwners = getStandardAppInfoOwners(context)
        val marketOwners = getMarketAppInfoOwners(context)
        val map = HashMap<String, AppInfoOwner>(standardOwners.size + marketOwners.size + 2).apply {
            // Google Play Store itself is a standard owner, this serves as a fallback
            put(GooglePlayAppInfoOwner.packageName, GooglePlayAppInfoOwner)
            // put standard owners afterwards to override custom owners, in reversed order
            // (in case one package / multiple activities, to retain the smaller-index one)
            putAll(standardOwners.asReversed().associateBy { it.packageName })
            // market owners will override standard ones as app stores
            putAll(marketOwners.asReversed().associateBy { it.packageName })
            // Xiaomi app store itself is a market owner that blocks app details intent,
            // use this custom owner instead as a workaround
            put(XiaomiAppInfoOwner.packageName, XiaomiAppInfoOwner)
            // remove self owner (though N/A right now)
            remove(BuildConfig.APPLICATION_ID)
            // remove unqualified owners
            for ((invalidPkg, invalidClassName) in UnqualifiedAppInfoOwners) {
                val addedOwner = get(invalidPkg) as? CompAppInfoOwner ?: continue
                if (addedOwner.comp.className == invalidClassName) remove(invalidPkg)
            }
            for ((invalidPkg, invalidClassName) in UnqualifiedAppStoreOwners) {
                val addedOwner = get(invalidPkg) as? CompAppInfoOwner ?: continue
                if (addedOwner.comp.className == invalidClassName) remove(invalidPkg)
            }
            // remove disabled or uninstalled owners
            keys.retainAll { pkg ->
                // check activity existence for custom AppInfoOwners
                (get(pkg) as? GooglePlayAppInfoOwner)?.let { addedOwner ->
                    val intent = Intent().setComponent(addedOwner.comp)
                    val info = context.packageManager.resolveActivity(intent, 0)
                    return@retainAll info?.activityInfo?.enabled == true
                }
                kotlin.runCatching { context.packageManager.getApplicationInfo(pkg, 0) }
                    .fold({ it.enabled }, { false })
            }
        }
        return map.values.toList()
    }
}

private const val BaiduSearchFileManager =
    "com.baidu.searchbox.download.center.ui.fusion.FileManagerActivity"
private val UnqualifiedAppInfoOwners: Map<String, String> = hashMapOf(
    // a MarketAppInfoOwner that queries packageName and downloads APK from its own source,
    // which would rather be qualified as ApkDownloader instead of AppInfoOwner
    "com.baidu.searchbox" to BaiduSearchFileManager,
    "com.baidu.searchbox.lite" to BaiduSearchFileManager,
    "com.baidu.searchbox.tomas" to BaiduSearchFileManager,
    // UC 神马应用, not actively operated, likely being replaced by PP助手
    "com.UCMobile" to "com.UCMobile.main.UCMobile",
    "com.ucmobile.lite" to "com.UCMobile.main.UCMobile",
    // QQ Browser, page not opening up
    "com.tencent.mtt" to "com.tencent.mtt.external.market.ui.QQMarketReceiveIntentActivity",
)

private val UnqualifiedAppStoreOwners: Map<String, String> = hashMapOf(
    "com.absinthe.libchecker" to "com.absinthe.libchecker.features.applist.detail.ui.AppDetailActivity",
    "com.catchingnow.icebox" to "com.catchingnow.icebox.activity.AppInfoActivity",
    "rikka.appops" to "rikka.appops.appdetail.AppDetailActivity",
)
