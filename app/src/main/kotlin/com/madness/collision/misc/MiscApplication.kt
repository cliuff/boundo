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

package com.madness.collision.misc

import android.content.ComponentName
import android.content.Context
import android.content.pm.ComponentInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.madness.collision.util.os.OsUtils

object MiscApplication {

    fun isComponentEnabled(context: Context, clsName: String, components: List<ComponentInfo> = getComponents(context)): Boolean {
        val pkgName = context.packageName
        val pm = context.packageManager
        val componentName = ComponentName(pkgName, clsName)
        return when (pm.getComponentEnabledSetting(componentName)) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED -> false
//            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
            else -> components.find { it.name == clsName }?.isEnabled ?: false
        }
    }

    fun getComponents(context: Context): List<ComponentInfo> {
        val pkgName = context.packageName
        val pm = context.packageManager
        val flagGetDisabled = if (OsUtils.satisfy(OsUtils.N)) PackageManager.MATCH_DISABLED_COMPONENTS
        else flagGetDisabledLegacy
        val flags = PackageManager.GET_ACTIVITIES or PackageManager.GET_RECEIVERS or
                PackageManager.GET_SERVICES or PackageManager.GET_PROVIDERS or flagGetDisabled
        try {
            val packageInfo: PackageInfo = pm.getPackageInfo(pkgName!!, flags)
            val components: MutableList<ComponentInfo> = mutableListOf()
            packageInfo.activities?.let { components.addAll(it) }
            packageInfo.services?.let { components.addAll(it) }
            packageInfo.providers?.let { components.addAll(it) }
            return components
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return emptyList()
    }

    @Suppress("deprecation")
    private val flagGetDisabledLegacy = PackageManager.GET_DISABLED_COMPONENTS
}
