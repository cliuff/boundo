/*
 * Copyright 2020 Clifford Liu
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
import android.view.View
import android.widget.Toast
import com.google.android.play.core.splitinstall.SplitInstallException
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.model.SplitInstallErrorCode
import com.madness.collision.R
import com.madness.collision.qs.TileServiceApiViewer
import com.madness.collision.qs.TileServiceAudioTimer
import com.madness.collision.unit.themed_wallpaper.ThemedWallpaperService
import com.madness.collision.unit.we_chat_evo.InstantWeChatActivity
import com.madness.collision.util.X
import com.madness.collision.util.notify
import com.madness.collision.util.notifyBriefly
import com.madness.collision.versatile.ApiViewingSearchActivity
import com.madness.collision.versatile.ApkSharing
import com.madness.collision.versatile.AppInfoWidget
import com.madness.collision.versatile.TextProcessingActivity
import java.lang.ref.WeakReference
import kotlin.reflect.KClass

internal class UnitManager(private val context: Context, private val splitInstallManager: SplitInstallManager) {

    private fun updateUnitState(description: Description) {
//        val index = mDescriptions.indexOf(description)
//        notifyListItemChanged(index)
    }

    fun installUnit(description: Description, view: View? = null) {
        val viewRef = WeakReference(view)
        val request: SplitInstallRequest = SplitInstallRequest.newBuilder().addModule(description.unitName).build()
        splitInstallManager.startInstall(request).addOnSuccessListener {
            viewRef.notify(R.string.unit_manager_install_success, true)
            updateUnitState(description)
            doAftermath(context, description, false)
        }.addOnFailureListener { exception ->
            val e = if (exception is SplitInstallException) exception else null
            val errorMessage = when(e?.errorCode) {
                SplitInstallErrorCode.NETWORK_ERROR -> R.string.unit_manager_error_network
                SplitInstallErrorCode.MODULE_UNAVAILABLE -> R.string.unit_manager_error_module_unavailable
                SplitInstallErrorCode.API_NOT_AVAILABLE -> R.string.unit_manager_error_api_unavailable
                else -> R.string.text_error
            }
            viewRef.notify(errorMessage)
            updateUnitState(description)
        }
    }

    fun uninstallUnit(description: Description, view: View? = null) {
        val viewRef = WeakReference(view)
        splitInstallManager.deferredUninstall(listOf(description.unitName)).addOnSuccessListener {
            viewRef.notify(R.string.unit_manager_uninstall_success, true)
            Unit.unpinUnit(context, description.unitName)
            updateUnitState(description)
            doAftermath(context, description, true)
        }.addOnFailureListener {
            viewRef.notify(R.string.text_error)
            Unit.unpinUnit(context, description.unitName)
            updateUnitState(description)
        }
    }

    private fun doAftermath(context: Context, description: Description, isUninstall: Boolean) {
        when (description.unitName) {
            Unit.UNIT_NAME_API_VIEWING -> this::aftermathApiViewing
            Unit.UNIT_NAME_AUDIO_TIMER -> this::aftermathAudioTimer
            Unit.UNIT_NAME_COOL_APP -> this::aftermathCoolApp
            Unit.UNIT_NAME_THEMED_WALLPAPER -> this::aftermathThemedWallpaper
            Unit.UNIT_NAME_WE_CHAT_EVO -> this::aftermathWeChatEvo
            else -> return
        }.invoke(context, isUninstall)
    }

    private fun WeakReference<View?>.notify(textResId: Int, shouldLastLong: Boolean = false) {
        val v = this.get()
        if (v != null) {
            (if (shouldLastLong) View::notify else View::notifyBriefly).invoke(v!!, textResId, true)
        } else {
            X.toast(context, textResId, if (shouldLastLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT)
        }
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

    private fun aftermathCoolApp(context: Context, isUninstall: Boolean) {
        context.packageManager.setComponentEnabledSetting(
                componentName<AppInfoWidget>(context),
                isUninstall.stateDisabled(),
                PackageManager.DONT_KILL_APP
        )
    }

    private fun aftermathApiViewing(context: Context, isUninstall: Boolean) {
        val state = isUninstall.stateDisabled()
        val pm = context.packageManager
        listOf(ApkSharing::class, TextProcessingActivity::class, ApiViewingSearchActivity::class, TileServiceApiViewer::class).forEach {
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

    private fun aftermathWeChatEvo(context: Context, isUninstall: Boolean) {
        context.packageManager.setComponentEnabledSetting(
                componentName<InstantWeChatActivity>(context),
                isUninstall.stateDisabled(),
                PackageManager.DONT_KILL_APP
        )
    }

}
