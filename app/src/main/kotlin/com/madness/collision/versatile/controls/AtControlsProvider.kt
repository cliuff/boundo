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
import android.service.controls.templates.RangeTemplate
import android.service.controls.templates.ToggleRangeTemplate
import com.madness.collision.R
import com.madness.collision.main.MainActivity
import com.madness.collision.unit.Unit
import com.madness.collision.util.SystemUtil
import com.madness.collision.versatile.ctrl.AudioTimerControlCreator
import com.madness.collision.versatile.ctrl.ControlActionRequest
import com.madness.collision.versatile.ctrl.ControlInfo
import com.madness.collision.versatile.ctrl.ControlStatus
import com.madness.collision.versatile.ctrl.TimerValues
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@TargetApi(Build.VERSION_CODES.R)
class AtControlsProvider : ControlsProvider {
    override val controlIdRegex: String = DEV_ID_AT
    private val timerValues = TimerValues(120f, 5f, 120f)
    private val creator = AudioTimerControlCreator(timerValues)

    companion object {
        const val DEV_ID_AT = "dev_at_timer"
    }

    override suspend fun getDeviceIds(): List<String> {
        return listOf(DEV_ID_AT)
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
            .putExtras(MainActivity.forItem(Unit.UNIT_NAME_AUDIO_TIMER))
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        return Control.StatelessBuilder(DEV_ID_AT, activityIntent(intent))
            .setDeviceType(DeviceTypes.TYPE_GENERIC_START_STOP)
            .applyInfo(info)
            .build()
    }

    private fun getStatefulControl(context: Context, info: ControlInfo): Control {
        val toggleRangeTemplate = when (info) {
            is ControlInfo.ButtonStatus -> timerValues.run {
                val localeContext = SystemUtil.getLocaleContextSys(context)
                val currentValDisplay = if (atCurrentValue < atStepValue) atStepValue else atCurrentValue
                val formatRes = R.string.versatile_device_controls_at_status_on
                val formatString = localeContext.getString(formatRes)
                val rangeTemplate = RangeTemplate(DEV_ID_AT, atStepValue, atMaxValue,
                    currentValDisplay, atStepValue, formatString)
                val controlButton = ControlButton(info.isChecked, info.actionDesc)
                ToggleRangeTemplate(DEV_ID_AT, controlButton, rangeTemplate)
            }
            else -> null
        }
        val stateless = getStatelessControl(context, info)
        return Control.StatefulBuilder(stateless)
            .setSubtitle(info.subtitle)
            .apply { if (info is ControlStatus) setStatusText(info.status) }
            .setStatus(Control.STATUS_OK)
            .apply { if (toggleRangeTemplate != null) setControlTemplate(toggleRangeTemplate) }
            .build()
    }
}
