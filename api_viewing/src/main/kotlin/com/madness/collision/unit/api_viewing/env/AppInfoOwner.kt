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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import com.madness.collision.util.F
import com.madness.collision.util.getProviderUri
import com.madness.collision.util.os.OsUtils

interface AppInfoOwner {
    val packageName: String
    fun showAppInfo(appPkgName: String, context: Context)
}

class StandardAppInfoOwner(override val packageName: String, val activityName: String) : AppInfoOwner {
    override fun showAppInfo(appPkgName: String, context: Context) {
        val intent = Intent(EnvPackages.ActionShowAppInfo)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(EnvPackages.ExtraPackageName, appPkgName)
            .setComponent(ComponentName(packageName, activityName))
        context.startActivity(intent)
    }
}

class MarketAppInfoOwner(override val packageName: String, val activityName: String) : AppInfoOwner {
    override fun showAppInfo(appPkgName: String, context: Context) {
        val uri = Uri.parse("market://details?id=$appPkgName")
        val intent = Intent(Intent.ACTION_VIEW, uri)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .setComponent(ComponentName(packageName, activityName))
        context.startActivity(intent)
    }
}

class SettingsAppInfoOwner(override val packageName: String) : AppInfoOwner {
    override fun showAppInfo(appPkgName: String, context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .setData(Uri.fromParts("package", appPkgName, null))
        context.startActivity(intent)
    }
}

object GooglePlayAppInfoOwner : AppInfoOwner {
    private const val packageGooglePlay = "com.android.vending"
    private const val infoActivity = "com.google.android.finsky.activities.MainActivity"
    override val packageName: String = packageGooglePlay

    override fun showAppInfo(appPkgName: String, context: Context) {
        val uri = Uri.parse("https://play.google.com/store/apps/details?id=$appPkgName")
        val intent = Intent(Intent.ACTION_VIEW, uri)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .setComponent(ComponentName(packageName, infoActivity))
        context.startActivity(intent)
    }
}

object CoolApkAppInfoOwner : AppInfoOwner {
    private const val packageCoolApk = "com.coolapk.market"
    private const val infoActivity = "com.coolapk.market.view.AppLinkActivity"
    override val packageName: String = packageCoolApk

    override fun showAppInfo(appPkgName: String, context: Context) {
        val uri = Uri.parse("https://www.coolapk.com/apk/$appPkgName")
        val intent = Intent(Intent.ACTION_VIEW, uri)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .setComponent(ComponentName(packageName, infoActivity))
        context.startActivity(intent)
    }
}

object XiaomiAppInfoOwner : AppInfoOwner {
    private const val packageXiaomiMarket = "com.xiaomi.market"
    override val packageName: String = packageXiaomiMarket

    override fun showAppInfo(appPkgName: String, context: Context) {
        val uri = Uri.parse("mimarket://details/detailmini?id=$appPkgName")
        val intent = Intent(Intent.ACTION_VIEW, uri)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .setPackage(packageName)
        context.startActivity(intent)
    }
}

object AppManagerAppInfoOwner : AppInfoOwner {
    private const val packageAppManager = "io.github.muntashirakon.AppManager"
    private const val infoActivity = "io.github.muntashirakon.AppManager.details.AppInfoActivity"
    private const val infoActivityLegacy = "io.github.muntashirakon.AppManager.details.AppDetailsActivity"
    override val packageName: String = packageAppManager

    @Suppress("deprecation")
    private fun PackageManager.resolveActivityLegacy(intent: Intent, flags: Int) =
        resolveActivity(intent, flags)

    override fun showAppInfo(appPkgName: String, context: Context) {
        val fakeApk = F.createFile(F.valCachePubAvApk(context), "fake.apk")
        val intent = Intent(Intent.ACTION_VIEW)
            .addCategory(Intent.CATEGORY_DEFAULT)
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .setData(fakeApk.getProviderUri(context))
            .setClassName(packageName, infoActivity)
            .putExtra("pkg", appPkgName)
        val pm = context.packageManager
        val info = when {
            OsUtils.dissatisfy(OsUtils.T) -> pm.resolveActivityLegacy(intent, 0)
            else -> pm.resolveActivity(intent, PackageManager.ResolveInfoFlags.of(0))
        }
        if (info == null) intent.setClassName(packageName, infoActivityLegacy)
        context.startActivity(intent)
    }
}
