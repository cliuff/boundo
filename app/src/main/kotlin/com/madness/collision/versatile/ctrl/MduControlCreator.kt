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

package com.madness.collision.versatile.ctrl

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.controls.actions.BooleanAction
import android.text.format.Formatter
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.madness.collision.R
import com.madness.collision.util.PermissionUtils
import com.madness.collision.util.SysServiceUtils
import com.madness.collision.util.SystemUtil
import com.madness.collision.util.notice.ToastUtils
import com.madness.collision.versatile.controls.CommonControl
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@RequiresApi(Build.VERSION_CODES.R)
class MduControlCreator : ControlCreator<ControlInfo> {
    override suspend fun create(context: Context, id: String): ControlInfo {
        return defaultDetails(context)
    }

    override fun create(context: Context, id: String, actionFlow: Flow<ControlActionRequest>): Flow<ControlInfo> {
        return channelFlow {
            send(getStatus(context, false))
            // when performing user-requested update action:
            // a. usage access not permitted: ask permission now
            // (avoid permission request as early as adding controls, which is unnecessary)
            // b. usage access permitted: show loading state first then update with real data
            actionFlow
                .filter { (_, action) -> action is BooleanAction }
                .onEach {
                    send(getStatus(context, true))
                    if (PermissionUtils.isUsageAccessPermitted(context).not()) {
                        getPermission(context)
                    } else {
                        delay(800)
                        send(getStatus(context, false))
                    }
                }
                .launchIn(this)
        }
    }

    private fun defaultDetails(context: Context): ControlDetails = CommonControl(context) {
        val localeContext = SystemUtil.getLocaleContextSys(context)
        ControlDetails(
            title = localeContext.getString(R.string.tileData),
            subtitle = localeContext.getString(R.string.versatile_device_controls_mdu_hint),
            icon = drawableIcon(R.drawable.ic_data_usage_24),
        )
    }

    private fun getStatus(context: Context, isUserAction: Boolean): ControlStatus {
        val status = when {
            PermissionUtils.isUsageAccessPermitted(context).not() -> {
                val localeContext = SystemUtil.getLocaleContextSys(context)
                localeContext.getString(R.string.text_access_denied)
            }
            isUserAction -> "…"
            else -> {
                val (totalDay, totalMonth) = SysServiceUtils.getDataUsage(context)
                val usageDay = Formatter.formatFileSize(context, totalDay)
                val usageMonth = Formatter.formatFileSize(context, totalMonth)
                "$usageDay • $usageMonth"
            }
        }
        return ControlStatus(defaultDetails(context), status)
    }

    private suspend fun getPermission(context: Context) {
        val intent = Intent().apply {
            action = Settings.ACTION_USAGE_ACCESS_SETTINGS
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        delay(1000)
        ToastUtils.toast(context, R.string.access_sys_usage, Toast.LENGTH_LONG)
    }
}