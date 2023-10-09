/*
 * Copyright 2021 Clifford Liu
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

package com.madness.collision.unit.api_viewing.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.madness.collision.chief.lang.mapIf
import com.madness.collision.util.SystemUtil
import com.madness.collision.util.config.LocaleUtils
import com.madness.collision.util.os.OsUtils
import java.util.*

object AppInfoProcessor {

    fun loadName(context: Context, applicationInfo: ApplicationInfo, overrideSystem: Boolean): String {
        val packageName = applicationInfo.packageName
        if (overrideSystem) loadLocaleLabel(context, packageName)?.let { return it }
        return context.packageManager.getApplicationLabel(applicationInfo).toString()
            .mapIf({ it.isEmpty() }, { packageName })  // use packageName instead when empty
    }

    private fun loadLocaleLabel(context: Context, pkgName: String): String? {
        // below: unable to create context for Android System
        if (pkgName == "android" || OsUtils.satisfy(OsUtils.T)) return null
        val locale = LocaleUtils.getSet()?.first()
        if (locale == null || locale == LocaleUtils.getApp()[0]) return null
        return loadLabel(context, pkgName, locale)
    }

    private fun loadLabel(context: Context, pkgName: String, locale: Locale): String? {
        val nContext: Context
        try {
            nContext = context.createPackageContext(pkgName, Context.CONTEXT_RESTRICTED)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            return null
        }
        val localeContext = SystemUtil.getLocaleContext(nContext, locale)
        val labelRes = localeContext.applicationInfo.labelRes
        if (labelRes == 0) return null
        try {
            return localeContext.getString(labelRes)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}