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
import android.os.Build
import android.service.controls.actions.BooleanAction
import android.service.controls.actions.ControlAction
import android.service.controls.actions.FloatAction
import androidx.annotation.RequiresApi
import com.madness.collision.R
import com.madness.collision.unit.audio_timer.AccessAT
import com.madness.collision.unit.audio_timer.AtCallback
import com.madness.collision.util.SystemUtil
import com.madness.collision.versatile.controls.CommonControl
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.math.roundToLong

@RequiresApi(Build.VERSION_CODES.R)
class AudioTimerControlCreator(private val timerValues: TimerValues) : ControlCreator<ControlInfo> {
    private var atCallback: AtCallback? = null
    // block update during timer duration change, which causes temporary shutdown of timer,
    // during which time constant regular state updates are happening,
    // which causes glitches because of rapid switching between states
    private var isAtUpdateBlocked = false

    override suspend fun create(context: Context, id: String): ControlInfo {
        return defaultDetails(context)
    }

    override fun create(context: Context, id: String, actionFlow: Flow<ControlActionRequest>): Flow<ControlInfo> {
        return channelFlow {
            ensureInit(context, channel)
            send(getStatus(context, AccessAT.isRunning()))
            actionFlow
                .onEach { (_, action) ->
                    ensureInit(context, channel)
                    val isRunning = resolveAction(action, context)
                    send(getStatus(context, isRunning))
                }
                .launchIn(this)
        }
    }

    private fun ensureInit(context: Context, channel: SendChannel<ControlInfo>) {
        val isRunning = AccessAT.isRunning()
        if (isRunning && atCallback == null) {
            val callback = getAtCallback(channel, context)
            AccessAT.addCallback(callback)
            atCallback = callback
        }
        if (!isRunning) {
            // reset current value
            timerValues.atCurrentValue = timerValues.atMaxValue
        }
    }

    private fun defaultDetails(context: Context): ControlDetails = CommonControl(context) {
        val localeContext = SystemUtil.getLocaleContextSys(context)
        ControlDetails(
            title = localeContext.getString(R.string.unit_audio_timer),
            subtitle = "",
            icon = drawableIcon(R.drawable.ic_timer_24),
        )
    }

    private fun getStatus(context: Context, isRunning: Boolean): ControlInfo.ButtonStatus {
        val localeContext = SystemUtil.getLocaleContextSys(context)
        return if (isRunning) {
            val sub = localeContext.getString(R.string.versatile_device_controls_at_hint_on)
            val actionDesc = localeContext.getString(R.string.versatile_device_controls_at_ctrl_desc_on)
            val details = defaultDetails(context).run { ControlDetails(title, sub, icon) }
            ControlInfo.ButtonStatus(details, "", isRunning, actionDesc)
        } else {
            val sub = localeContext.getString(R.string.versatile_device_controls_at_hint_off)
            val status = localeContext.getString(R.string.versatile_device_controls_at_status_off)
            val actionDesc = localeContext.getString(R.string.versatile_device_controls_at_ctrl_desc_off)
            val details = defaultDetails(context).run { ControlDetails(title, sub, icon) }
            ControlInfo.ButtonStatus(details, status, isRunning, actionDesc)
        }
    }

    private fun getAtCallback(channel: SendChannel<ControlInfo>, context: Context)
    = object : AtCallback {
        private val timerState = TimerState(timerValues)

        override fun onTick(targetTime: Long, duration: Long, leftTime: Long) {
            // when timer stops
            if (leftTime < 1) {
                AccessAT.removeCallback(this)
                atCallback = null
                timerState.stop()
                return
            }
            val (lastCurrentValue, newCurrentValue, stepValue) = timerState.updateValues(leftTime)
            val doUpdate = lastCurrentValue - newCurrentValue >= stepValue
            // update control status
            if (!isAtUpdateBlocked && doUpdate) {
                channel.trySend(getStatus(context, AccessAT.isRunning()))
            }
        }

        override fun onTick(displayText: String) {
        }
    }

    private suspend fun resolveAction(action: ControlAction, context: Context): Boolean {
        val isRunning = AccessAT.isRunning()
        when(action) {
            is BooleanAction -> {
                val isStarting = action.newState
                if (isStarting && !isRunning) {
                    AccessAT.start(context, timerValues.atCurrentValue.roundToLong() * 60000)
                } else if (isRunning) {
                    AccessAT.stop(context)
                    // reset current value
                    timerValues.atCurrentValue = timerValues.atMaxValue
                }
                return isStarting
            }
            is FloatAction -> {
                if (timerValues.atCurrentValue != action.newValue) {
                    // block update
                    isAtUpdateBlocked = true
                    AccessAT.stop(context)
                    timerValues.atCurrentValue = action.newValue
                    coroutineScope {
                        delay(500)
                        AccessAT.start(context, timerValues.atCurrentValue.roundToLong() * 60000)
                        delay(500)
                        isAtUpdateBlocked = false
                    }
                    return true
                }
            }
        }
        return isRunning
    }
}