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

package com.madness.collision.versatile.controls

import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.controls.Control
import android.service.controls.DeviceTypes
import android.service.controls.actions.BooleanAction
import android.service.controls.actions.ControlAction
import android.service.controls.templates.ControlButton
import android.service.controls.templates.ToggleTemplate
import android.text.format.Formatter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toIcon
import com.madness.collision.R
import com.madness.collision.main.MainActivity
import com.madness.collision.util.PermissionUtils
import com.madness.collision.util.SysServiceUtils
import com.madness.collision.util.SystemUtil
import com.madness.collision.util.notice.ToastUtils
import com.madness.collision.util.os.OsUtils
import io.reactivex.rxjava3.processors.ReplayProcessor
import kotlinx.coroutines.*

@TargetApi(Build.VERSION_CODES.R)
class MonthDataUsageControlProvider(private val context: Context) : ControlProvider {
    override val controlIdRegex: String = DEV_ID_MDU
    private val coroutineScope = CoroutineScope(Dispatchers.Default + Job())

    companion object {
        const val DEV_ID_MDU = "dev_mdu_data"
    }

    override fun onDestroy() {
        coroutineScope.cancel()
        super.onDestroy()
    }

    override suspend fun getDeviceIds(): List<String> {
        return listOf(DEV_ID_MDU)
    }

    override suspend fun getStatelessControl(controlId: String): Control {
        return getStatelessControl(context)
    }

    override fun getStatefulControl(updatePublisher: ReplayProcessor<Control>, controlId: String, action: ControlAction?) {
        val control = getControl(updatePublisher, controlId, action)
        updatePublisher.onNext(control)
    }

    private fun getControl(updatePublisher: ReplayProcessor<Control>, controlId: String, action: ControlAction?): Control {
        return getMduControl(updatePublisher, controlId, action).build()
    }

    private fun getStatelessControl(context: Context): Control {
        val localeContext = SystemUtil.getLocaleContextSys(context)
        val intent = Intent(context, MainActivity::class.java)
        val piMutabilityFlag = if (OsUtils.satisfy(OsUtils.M)) PendingIntent.FLAG_IMMUTABLE else 0
        val pi = PendingIntent.getActivity(context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or piMutabilityFlag)
        val color = context.getColor(R.color.primaryAWhite)
        val title = localeContext.getString(R.string.tileData)
        val iconDrawable = ContextCompat.getDrawable(context, R.drawable.ic_data_usage_24)
        iconDrawable?.setTint(color)
        val icon = iconDrawable?.toBitmap()?.toIcon()
        return Control.StatelessBuilder(DEV_ID_MDU, pi)
            .setTitle(title)
            .setSubtitle(context.getString(R.string.versatile_device_controls_mdu_hint))
            .setDeviceType(DeviceTypes.TYPE_GENERIC_VIEWSTREAM)
            .setCustomIcon(icon)
            .build()
    }

    private fun getStatefulControl(context: Context): Control {
        val stateless = getStatelessControl(context)
        val localeContext = SystemUtil.getLocaleContextSys(context)
        val actionDesc = localeContext.getString(R.string.versatile_device_controls_mdu_ctrl_desc)
        val controlButton = ControlButton(false, actionDesc)
        val template = ToggleTemplate(DEV_ID_MDU, controlButton)
        return Control.StatefulBuilder(stateless)
            .setStatus(Control.STATUS_OK)
            .setControlTemplate(template)
            .build()
    }

    private fun getMduControl(updatePublisher: ReplayProcessor<Control>, controlId: String, action: ControlAction?): Control.StatefulBuilder {
        val localeContext = SystemUtil.getLocaleContextSys(context)
        val hasAccess = PermissionUtils.isUsageAccessPermitted(context)
        val doUpdate = action != null && action is BooleanAction
        // ask permission only when performing user-requested update action
        // to avoid permission request when adding controls, which is too early and unnecessary
        if (doUpdate && !hasAccess) getPermission(context)
        if (doUpdate && hasAccess) {
            coroutineScope.launch {
                delay(800)
                val control = getControl(updatePublisher, controlId, null)
                updatePublisher.onNext(control)
            }
        }
        val status = if (!hasAccess) {
            localeContext.getString(R.string.text_access_denied)
        } else if (doUpdate) {
            "…"
        } else {
            val (totalDay, totalMonth) = SysServiceUtils.getDataUsage(context)
            val usageDay = Formatter.formatFileSize(context, totalDay)
            val usageMonth = Formatter.formatFileSize(context, totalMonth)
            "$usageDay • $usageMonth"
        }
        val stateful = getStatefulControl(context)
        return Control.StatefulBuilder(stateful).setStatusText(status)
    }

    private fun getPermission(context: Context) {
        val intent = Intent().apply {
            action = Settings.ACTION_USAGE_ACCESS_SETTINGS
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        coroutineScope.launch {
            delay(1000)
            ToastUtils.toast(context, R.string.access_sys_usage, Toast.LENGTH_LONG)
        }
    }
}