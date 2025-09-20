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

package com.madness.collision.unit

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.madness.collision.qs.TileServiceAudioTimer
import com.madness.collision.unit.themed_wallpaper.ThemedWallpaperService
import com.madness.collision.versatile.ApiViewingSearchActivity
import com.madness.collision.versatile.ApkSharing
import com.madness.collision.versatile.TextProcessingActivity
import kotlin.reflect.KClass

internal class UnitManager(private val context: Context) {

    fun enableUnit(description: Description) {
        Unit.enableUnit(context, description.unitName)
        doAftermath(context, description, false)
    }

    fun disableUnit(description: Description) {
        Unit.disableUnit(context, description.unitName)
        doAftermath(context, description, true)
    }

    private fun doAftermath(context: Context, description: Description, isUninstall: Boolean) {
        when (description.unitName) {
            Unit.UNIT_NAME_API_VIEWING -> this::aftermathApiViewing
            Unit.UNIT_NAME_AUDIO_TIMER -> this::aftermathAudioTimer
            Unit.UNIT_NAME_THEMED_WALLPAPER -> this::aftermathThemedWallpaper
            else -> return
        }.invoke(context, isUninstall)
    }

    private fun Boolean.stateEnabled(): Int {
        return if (this)
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        else
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    }

    private fun Boolean.stateDisabled(): Int {
        return (!this).stateEnabled()
    }

    private fun KClass<*>.toComponentName(context: Context): ComponentName {
        return ComponentName(context.packageName, qualifiedName ?: "")
    }

    private inline fun <reified T> componentName(context: Context): ComponentName {
        return T::class.toComponentName(context)
    }

    private fun aftermathApiViewing(context: Context, isUninstall: Boolean) {
        val state = isUninstall.stateDisabled()
        val pm = context.packageManager
        listOf(ApkSharing::class, TextProcessingActivity::class, ApiViewingSearchActivity::class).forEach {
            pm.setComponentEnabledSetting(it.toComponentName(context), state, PackageManager.DONT_KILL_APP)
        }
    }

    private fun aftermathThemedWallpaper(context: Context, isUninstall: Boolean) {
        context.packageManager.setComponentEnabledSetting(
                componentName<ThemedWallpaperService>(context),
                isUninstall.stateDisabled(),
                PackageManager.DONT_KILL_APP
        )
    }

    private fun aftermathAudioTimer(context: Context, isUninstall: Boolean) {
        context.packageManager.setComponentEnabledSetting(
                componentName<TileServiceAudioTimer>(context),
                isUninstall.stateDisabled(),
                PackageManager.DONT_KILL_APP
        )
    }

}
