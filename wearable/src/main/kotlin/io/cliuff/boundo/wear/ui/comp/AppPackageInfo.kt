/*
 * Copyright 2022 Clifford Liu
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

package io.cliuff.boundo.wear.ui.comp

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import io.cliuff.boundo.conf.coil.CompactPackageInfo
import io.cliuff.boundo.wear.model.ApiViewingApp

internal class AppPackageInfo(private val context: Context, private val appInfo: ApiViewingApp) :
    CompactPackageInfo {
    override val handleable: Boolean = true
    override val verCode: Long = appInfo.verCode
    override val uid: Int = appInfo.uid
    override val packageName: String = appInfo.packageName
    override fun loadUnbadgedIcon(pm: PackageManager): Drawable {
        val app = getApplicationInfo(context, packageName) ?: throw Exception("Application info is null")
        return app.loadUnbadgedIcon(pm)
    }
}

private fun getApplicationInfo(context: Context, pkgName: String): ApplicationInfo? {
    return try {
        if (pkgName.isEmpty()) return null
        context.packageManager.getApplicationInfo(pkgName, 0)
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
        null
    }
}
