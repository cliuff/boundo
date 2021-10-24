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
import android.content.pm.PackageManager
import com.madness.collision.settings.LanguageMan

object AppInfoProcessor {
    fun loadLabel(context: Context, pkgName: String, langCode: String): String? {
        val nContext: Context
        try {
            nContext = context.createPackageContext(pkgName, Context.CONTEXT_RESTRICTED)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            return null
        }
        val localeContext = LanguageMan.getLocaleContext(nContext, langCode)
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