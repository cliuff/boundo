package com.madness.collision.unit

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import com.google.android.play.core.splitinstall.SplitInstallException
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.model.SplitInstallErrorCode
import com.madness.collision.BuildConfig
import com.madness.collision.R
import com.madness.collision.util.X

internal class UnitManager(private val context: Context, private val splitInstallManager: SplitInstallManager) {

    private fun updateUnitState(description: Description) {
//        val index = mDescriptions.indexOf(description)
//        notifyListItemChanged(index)
    }

    fun installUnit(description: Description) {
        val request: SplitInstallRequest = SplitInstallRequest.newBuilder().addModule(description.unitName).build()
        splitInstallManager.startInstall(request).addOnSuccessListener {
            updateUnitState(description)
            X.toast(context, R.string.unit_manager_install_success, Toast.LENGTH_LONG)
            if (description.unitName == Unit.UNIT_NAME_COOL_APP) aftermathCoolApp(context, false)
        }.addOnFailureListener { exception ->
            updateUnitState(description)
            if (exception !is SplitInstallException) {
                X.toast(context, R.string.text_error, Toast.LENGTH_SHORT)
                return@addOnFailureListener
            }
            val errorMessage = when(exception.errorCode) {
                SplitInstallErrorCode.NETWORK_ERROR -> R.string.unit_manager_error_network
                SplitInstallErrorCode.MODULE_UNAVAILABLE -> R.string.unit_manager_error_module_unavailable
                SplitInstallErrorCode.API_NOT_AVAILABLE -> R.string.unit_manager_error_api_unavailable
                else -> R.string.text_error
            }
            X.toast(context, errorMessage, Toast.LENGTH_SHORT)
        }
    }

    fun uninstallUnit(description: Description) {
        splitInstallManager.deferredUninstall(listOf(description.unitName)).addOnSuccessListener {
            updateUnitState(description)
            X.toast(context, R.string.unit_manager_uninstall_success, Toast.LENGTH_LONG)
            if (description.unitName == Unit.UNIT_NAME_COOL_APP) aftermathCoolApp(context, true)
        }.addOnFailureListener {
            updateUnitState(description)
            X.toast(context, R.string.text_error, Toast.LENGTH_LONG)
        }
    }

    private fun aftermathCoolApp(context: Context, isUninstall: Boolean) {
        val state = if (isUninstall)
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        else
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        val componentName = ComponentName(context.packageName, "${ BuildConfig.BUILD_PACKAGE }.versatile.AppInfoWidget")
        context.packageManager.setComponentEnabledSetting(componentName, state, PackageManager.DONT_KILL_APP)
    }

}
