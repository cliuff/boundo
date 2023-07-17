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
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.controls.Control
import android.service.controls.DeviceTypes
import android.service.controls.templates.ControlButton
import android.service.controls.templates.ToggleTemplate
import com.madness.collision.R
import com.madness.collision.main.MainActivity
import com.madness.collision.util.SystemUtil
import com.madness.collision.versatile.ctrl.ControlActionRequest
import com.madness.collision.versatile.ctrl.ControlInfo
import com.madness.collision.versatile.ctrl.ControlStatus
import com.madness.collision.versatile.ctrl.MduControlCreator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@TargetApi(Build.VERSION_CODES.R)
class MduControlsProvider : ControlsProvider {
    override val controlIdRegex: String = DEV_ID_MDU
    private val creator = MduControlCreator()

    companion object {
        const val DEV_ID_MDU = "dev_mdu_data"
    }

    override suspend fun getDeviceIds(): List<String> {
        return listOf(DEV_ID_MDU)
    }

    override suspend fun create(context: Context, id: String): Control {
        val info = creator.create(context, id)
        return getStatelessControl(context, info)
    }

    override fun create(context: Context, id: String, actionFlow: Flow<ControlActionRequest>): Flow<Control> {
        return creator.create(context, id, actionFlow)
            .map { getStatefulControl(context, it) }
    }

    private fun getStatelessControl(context: Context, info: ControlInfo): Control = CommonControl(context) {
        val intent = Intent(context, MainActivity::class.java)
        return Control.StatelessBuilder(DEV_ID_MDU, activityIntent(intent))
            .setDeviceType(DeviceTypes.TYPE_GENERIC_VIEWSTREAM)
            .applyInfo(info)
            .build()
    }

    private fun getStatefulControl(context: Context, info: ControlInfo): Control {
        val stateless = getStatelessControl(context, info)
        val localeContext = SystemUtil.getLocaleContextSys(context)
        val actionDesc = localeContext.getString(R.string.versatile_device_controls_mdu_ctrl_desc)
        val controlButton = ControlButton(false, actionDesc)
        val template = ToggleTemplate(DEV_ID_MDU, controlButton)
        return Control.StatefulBuilder(stateless)
            .apply { if (info is ControlStatus) setStatusText(info.status) }
            .setStatus(Control.STATUS_OK)
            .setControlTemplate(template)
            .build()
    }
}
