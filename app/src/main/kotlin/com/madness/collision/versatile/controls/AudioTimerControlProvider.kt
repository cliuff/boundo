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
import android.service.controls.Control
import android.service.controls.DeviceTypes
import android.service.controls.actions.BooleanAction
import android.service.controls.actions.ControlAction
import android.service.controls.actions.FloatAction
import android.service.controls.templates.ControlButton
import android.service.controls.templates.RangeTemplate
import android.service.controls.templates.ToggleRangeTemplate
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toIcon
import com.madness.collision.R
import com.madness.collision.main.MainActivity
import com.madness.collision.unit.Unit
import com.madness.collision.unit.audio_timer.AccessAT
import com.madness.collision.unit.audio_timer.AtCallback
import com.madness.collision.util.SystemUtil
import com.madness.collision.util.os.OsUtils
import io.reactivex.rxjava3.processors.ReplayProcessor
import kotlinx.coroutines.*
import kotlin.math.min
import kotlin.math.roundToLong

@TargetApi(Build.VERSION_CODES.R)
class AudioTimerControlProvider(private val context: Context) : ControlProvider {
    override val controlIdRegex: String = DEV_ID_AT
    private val coroutineScope = CoroutineScope(Dispatchers.Default + Job())
    private val atMaxValue = 120f
    private val atStepValue = 5f
    private var atCurrentValue = atMaxValue
    private var atCallback: AtCallback? = null
    // block update during timer duration change, which causes temporary shutdown of timer,
    // during which time constant regular state updates are happening,
    // which causes glitches because of rapid switching between states
    private var isAtUpdateBlocked = false

    companion object {
        const val DEV_ID_AT = "dev_at_timer"
    }

    override fun onDestroy() {
        coroutineScope.cancel()
        super.onDestroy()
    }

    override suspend fun getDeviceIds(): List<String> {
        return listOf(DEV_ID_AT)
    }

    override suspend fun getStatelessControl(controlId: String): Control {
        return getStatelessControl(context)
    }

    override fun getStatefulControl(updatePublisher: ReplayProcessor<Control>, controlId: String, action: ControlAction?) {
        val control = getControl(updatePublisher, controlId, action)
        updatePublisher.onNext(control)
    }

    private fun getControl(updatePublisher: ReplayProcessor<Control>, controlId: String, action: ControlAction?): Control {
        var isRunning = AccessAT.isRunning()
        if (isRunning && atCallback == null) {
            val callback = getAtCallback(updatePublisher, controlId)
            AccessAT.addCallback(callback)
            atCallback = callback
        }
        if (!isRunning) {
            // reset current value
            atCurrentValue = atMaxValue
        }
        if (action != null) isRunning = resolveAtAction(action, isRunning, context)
        return getStatefulControl(context, isRunning)
    }

    private fun getStatelessControl(context: Context): Control {
        val localeContext = SystemUtil.getLocaleContextSys(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtras(MainActivity.forItem(Unit.UNIT_NAME_AUDIO_TIMER))
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val piMutabilityFlag = if (OsUtils.satisfy(OsUtils.M)) PendingIntent.FLAG_IMMUTABLE else 0
        val pi = PendingIntent.getActivity(context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or piMutabilityFlag)
        val color = context.getColor(R.color.primaryAWhite)
        val title = localeContext.getString(R.string.unit_audio_timer)
        val iconDrawable = ContextCompat.getDrawable(context, R.drawable.ic_timer_24)
        iconDrawable?.setTint(color)
        val icon = iconDrawable?.toBitmap()?.toIcon()
        return Control.StatelessBuilder(DEV_ID_AT, pi)
            .setTitle(title)
            .setDeviceType(DeviceTypes.TYPE_GENERIC_START_STOP)
            .setCustomIcon(icon)
            .build()
    }

    private fun getStatefulControl(context: Context, isRunning: Boolean): Control {
        val localeContext = SystemUtil.getLocaleContextSys(context)
        val currentValDisplay = if (atCurrentValue < atStepValue) atStepValue else atCurrentValue
        val formatRes = R.string.versatile_device_controls_at_status_on
        val formatString = localeContext.getString(formatRes)
        val subRes = if (isRunning) R.string.versatile_device_controls_at_hint_on
        else R.string.versatile_device_controls_at_hint_off
        val sub = localeContext.getString(subRes)
        val status = if (isRunning) ""
        else localeContext.getString(R.string.versatile_device_controls_at_status_off)
        val rangeTemplate = RangeTemplate(DEV_ID_AT, atStepValue, atMaxValue,
            currentValDisplay, atStepValue, formatString)
        val actionDescRes = if (isRunning) R.string.versatile_device_controls_at_ctrl_desc_on
        else R.string.versatile_device_controls_at_ctrl_desc_off
        val actionDesc = localeContext.getString(actionDescRes)
        val controlButton = ControlButton(isRunning, actionDesc)
        val toggleRangeTemplate = ToggleRangeTemplate(DEV_ID_AT, controlButton, rangeTemplate)
//        val modeFlags = TemperatureControlTemplate.FLAG_MODE_COOL or
//                TemperatureControlTemplate.FLAG_MODE_HEAT or
//                TemperatureControlTemplate.FLAG_MODE_ECO
//        val template = TemperatureControlTemplate(DEV_ID_AT, toggleRangeTemplate,
//                TemperatureControlTemplate.MODE_COOL,
//                TemperatureControlTemplate.MODE_COOL, modeFlags)
        val stateless = getStatelessControl(context)
        return Control.StatefulBuilder(stateless)
            .setSubtitle(sub)
            .setStatusText(status)
            .setControlTemplate(toggleRangeTemplate)
            .setStatus(Control.STATUS_OK)
            .build()
    }

    private fun getAtCallback(updatePublisher: ReplayProcessor<Control>, controlId: String)
    = object : AtCallback {
        private var isChecked = false

        override fun onTick(targetTime: Long, duration: Long, leftTime: Long) {
            // when timer stops
            if (leftTime < 1) {
                AccessAT.removeCallback(this)
                atCallback = null
                atCurrentValue = atMaxValue
                return
            }
            val leftMin = (leftTime / 60000).toFloat()
            val newCurrentValue = min(leftMin, atMaxValue)
            // check real current value
            val doUpdate = atCurrentValue - newCurrentValue >= if (!isChecked) {
                isChecked = true
                0.02f // 0.02 min -> 1200 ms
            } else {
                atStepValue
            }
            atCurrentValue = newCurrentValue
            // update control status
            if (!isAtUpdateBlocked && doUpdate) updatePublisher.let {
                val control = getControl(it, controlId, null)
                it.onNext(control)
            }
        }

        override fun onTick(displayText: String) {
        }
    }

    private fun resolveAtAction(action: ControlAction, isRunning: Boolean, context: Context): Boolean {
        when(action) {
            is BooleanAction -> {
                val isStarting = action.newState
                if (isStarting && !isRunning) {
                    AccessAT.start(context, atCurrentValue.roundToLong() * 60000)
                } else if (isRunning) {
                    AccessAT.stop(context)
                    // reset current value
                    atCurrentValue = atMaxValue
                }
                return isStarting
            }
            is FloatAction -> {
                if (atCurrentValue != action.newValue) {
                    // block update
                    isAtUpdateBlocked = true
                    AccessAT.stop(context)
                    atCurrentValue = action.newValue
                    coroutineScope.launch {
                        delay(500)
                        AccessAT.start(context, atCurrentValue.roundToLong() * 60000)
                        delay(500)
                        isAtUpdateBlocked = false
                    }
                    return true
                }
            }
//            is ModeAction -> {
//            }
        }
        return isRunning
    }
}