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
import com.madness.collision.util.X
import com.madness.collision.util.notify
import com.madness.collision.util.notifyBriefly
import com.madness.collision.versatile.AppInfoWidget
import java.lang.ref.WeakReference

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
            if (description.unitName == Unit.UNIT_NAME_COOL_APP) aftermathCoolApp(context, false)
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
            updateUnitState(description)
            if (description.unitName == Unit.UNIT_NAME_COOL_APP) aftermathCoolApp(context, true)
        }.addOnFailureListener {
            viewRef.notify(R.string.text_error)
            updateUnitState(description)
        }
    }

    private fun WeakReference<View?>.notify(textResId: Int, shouldLastLong: Boolean = false) {
        val v = this.get()
        if (v != null) {
            (if (shouldLastLong) View::notify else View::notifyBriefly).invoke(v!!, textResId, true)
        } else {
            X.toast(context, textResId, if (shouldLastLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT)
        }
    }

    private fun aftermathCoolApp(context: Context, isUninstall: Boolean) {
        val state = if (isUninstall)
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        else
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        val componentName = ComponentName(context.packageName, AppInfoWidget::class.qualifiedName ?: "")
        context.packageManager.setComponentEnabledSetting(componentName, state, PackageManager.DONT_KILL_APP)
    }

}
