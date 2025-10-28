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
import com.madness.collision.main.MainActivity
import com.madness.collision.versatile.ctrl.ControlActionRequest
import com.madness.collision.versatile.ctrl.ControlInfo
import com.madness.collision.versatile.ctrl.ControlStatus
import com.madness.collision.versatile.ctrl.DevManControlCreator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@TargetApi(Build.VERSION_CODES.R)
class DevManControlsProvider(private val context: Context) : ControlsProvider {
    override val controlIdRegex: String = DevManControlCreator.IdRegex
    private val creator = DevManControlCreator()

    override fun onCreate() {
        creator.onCreate(context)
    }

    override fun onDestroy() {
        creator.close(context)
    }

    override suspend fun getDeviceIds(): List<String> {
        return creator.getDeviceIds(context)
    }

    override suspend fun create(context: Context, id: String): Control? {
        val info = creator.create(context, id) ?: return null
        return getStatelessControl(context, id, info)
    }

    override fun create(context: Context, id: String, actionFlow: Flow<ControlActionRequest>): Flow<Control> {
        return creator.create(context, id, actionFlow)
            .map { getStatefulControl(context, id, it) }
    }

    private fun getStatelessControl(context: Context, controlId: String, info: ControlInfo): Control = CommonControl(context) {
        // todo redirect to manage devices
        val intent = Intent(context, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        return Control.StatelessBuilder(controlId, activityIntent(intent))
            .setDeviceType(DeviceTypes.TYPE_GENERIC_ON_OFF)
            .applyInfo(info)
            .build()
    }

    private fun getStatefulControl(context: Context, controlId: String, info: ControlInfo): Control {
        val toggleTemplate = when (info) {
            is ControlInfo.ButtonStatus -> {
                val controlButton = ControlButton(info.isChecked, info.actionDesc)
                ToggleTemplate(controlId, controlButton)
            }
            else -> null
        }
        val stateless = getStatelessControl(context, controlId, info)
        return Control.StatefulBuilder(stateless)
            .apply { if (info is ControlStatus) setStatusText(info.status) }
            .setStatus(Control.STATUS_OK)
            .apply { if (toggleTemplate != null) setControlTemplate(toggleTemplate) }
            .build()
    }
}
