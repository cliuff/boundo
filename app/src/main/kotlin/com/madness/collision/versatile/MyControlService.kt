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

package com.madness.collision.versatile

import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.Icon
import android.service.controls.Control
import android.service.controls.ControlsProviderService
import android.service.controls.DeviceTypes
import android.service.controls.actions.BooleanAction
import android.service.controls.actions.ControlAction
import android.service.controls.actions.FloatAction
import android.service.controls.templates.*
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import com.madness.collision.R
import com.madness.collision.main.MainActivity
import com.madness.collision.unit.Unit
import com.madness.collision.unit.audio_timer.AccessAT
import com.madness.collision.unit.audio_timer.AtCallback
import com.madness.collision.util.SystemUtil
import com.madness.collision.util.X
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.processors.ReplayProcessor
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.reactivestreams.FlowAdapters
import java.util.concurrent.Flow
import java.util.function.Consumer
import kotlin.math.min
import kotlin.math.roundToLong

@TargetApi(X.R)
class MyControlService : ControlsProviderService() {
    companion object {
        private const val DEV_ID_AT = "dev_at_timer"
        private const val DEV_ID_MDU = "dev_mdu_data"

        private val DEVICE_IDS = listOf(DEV_ID_AT, DEV_ID_MDU)

        private fun getStatefulBuilder(context: Context, controlId: String): Control.StatefulBuilder? {
            val localeContext = SystemUtil.getLocaleContextSys(context)
            return when(controlId) {
                DEV_ID_AT -> {
                    val intent = Intent(context, MainActivity::class.java).putExtras(
                            MainActivity.forItem(Unit.UNIT_NAME_AUDIO_TIMER)
                    )
                    val pi = PendingIntent.getActivity(context, 0, intent,
                            PendingIntent.FLAG_UPDATE_CURRENT)
                    val color = context.getColor(R.color.primaryABlack)
                    val colorBack = ColorStateList.valueOf(context.getColor(R.color.primaryVBackABlack))
                    val title = SpannableString(localeContext.getString(R.string.unit_audio_timer))
                    title.setSpan(ForegroundColorSpan(color), 0, title.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    Control.StatefulBuilder(DEV_ID_AT, pi)
                            .setTitle(title)
                            .setDeviceType(DeviceTypes.TYPE_GENERIC_START_STOP)
                            .setStatus(Control.STATUS_OK)
                            .setCustomIcon(Icon.createWithResource(context, R.drawable.ic_timer_24).setTint(color))
                            .setCustomColor(colorBack)
                }
                DEV_ID_MDU -> {
                    val intent = Intent(context, MainActivity::class.java)
                    val pi = PendingIntent.getActivity(context, 0, intent,
                            PendingIntent.FLAG_UPDATE_CURRENT)
                    val controlButton = ControlButton(false, "Refresh data")
                    val color = context.getColor(R.color.primaryABlack)
                    val colorBack = ColorStateList.valueOf(context.getColor(R.color.primaryVBackABlack))
                    val title = SpannableString(localeContext.getString(R.string.tileData))
                    title.setSpan(ForegroundColorSpan(color), 0, title.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    val template = ToggleTemplate(DEV_ID_MDU, controlButton)
                    Control.StatefulBuilder(DEV_ID_MDU, pi)
                            .setTitle(title)
                            .setSubtitle("Tap to refresh")
                            .setDeviceType(DeviceTypes.TYPE_GENERIC_VIEWSTREAM)
                            .setStatus(Control.STATUS_OK)
                            .setCustomIcon(Icon.createWithResource(context, R.drawable.ic_data_usage_24).setTint(color))
                            .setCustomColor(colorBack)
                            .setControlTemplate(template)
                }
                else -> null
            }
        }

        private fun getStatelessBuilder(context: Context, controlId: String): Control.StatelessBuilder? {
            return null
        }
    }

    private lateinit var updatePublisher: ReplayProcessor<Control>
    private val atMaxValue = 120f
    private val atStepValue = 5f
    private var atCurrentValue = atMaxValue
    private var atCallback: AtCallback? = null
    // block update during timer duration change, which causes temporary shutdown of timer,
    // during which time constant regular state updates are happening,
    // which causes glitches because of rapid switching between states
    private var isAtUpdateBlocked = false

    override fun createPublisherForAllAvailable(): Flow.Publisher<Control> {
        val context = baseContext
        val controls = DEVICE_IDS.mapNotNull { getControl(context, it) }
        return FlowAdapters.toFlowPublisher(Flowable.fromIterable(controls))
    }

    override fun createPublisherFor(controlIds: MutableList<String>): Flow.Publisher<Control> {
        val context = baseContext
        updatePublisher = ReplayProcessor.create()
        controlIds.forEach {
            getControl(context, it)?.let { control ->
                updatePublisher.onNext(control)
            }
        }
        return FlowAdapters.toFlowPublisher(updatePublisher)
    }

    override fun performControlAction(controlId: String, action: ControlAction, consumer: Consumer<Int>) {
        val context = baseContext
        // Inform SystemUI that the action has been received and is being processed
        consumer.accept(ControlAction.RESPONSE_OK)
        getControl(context, controlId, action)?.let { control ->
            updatePublisher.onNext(control)
        }
    }

    private fun getControl(context: Context, controlId: String, action: ControlAction? = null): Control? {
        val localeContext = SystemUtil.getLocaleContextSys(context)
        return when(controlId) {
            DEV_ID_AT -> {
                var isRunning = AccessAT.isRunning()
                if (isRunning && atCallback == null) {
                    val callback = object : AtCallback {
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
                            if (!isAtUpdateBlocked && doUpdate) GlobalScope.launch {
                                getControl(context, controlId)?.let { control ->
                                    updatePublisher.onNext(control)
                                }
                            }
                        }

                        override fun onTick(displayText: String) {
                        }
                    }
                    AccessAT.addCallback(callback)
                    atCallback = callback
                }
                if (!isRunning) {
                    // reset current value
                    atCurrentValue = atMaxValue
                }
                if (action != null) {
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
                            isRunning = isStarting
                        }
                        is FloatAction -> {
                            if (atCurrentValue != action.newValue) {
                                // block update
                                isAtUpdateBlocked = true
                                AccessAT.stop(context)
                                atCurrentValue = action.newValue
                                GlobalScope.launch {
                                    delay(500)
                                    AccessAT.start(context, atCurrentValue.roundToLong() * 60000)
                                    delay(500)
                                    isAtUpdateBlocked = false
                                }
                                isRunning = true
                            }
                        }
//                        is ModeAction -> {
//                        }
                    }
                }
                val sub = if (isRunning) "Swipe to adjust" else "Tap to start"
                val status = if (isRunning) "" else "Stopped"
                val currentValDisplay = if (atCurrentValue < atStepValue) atStepValue else atCurrentValue
                val rangeTemplate = RangeTemplate(DEV_ID_AT, atStepValue, atMaxValue,
                        currentValDisplay, atStepValue, "%.0f min")
                val actionDesc = if (isRunning) "Stop timer" else "Start timer"
                val controlButton = ControlButton(isRunning, actionDesc)
                val toggleRangeTemplate = ToggleRangeTemplate(DEV_ID_AT, controlButton, rangeTemplate)
//                val modeFlags = TemperatureControlTemplate.FLAG_MODE_COOL or
//                        TemperatureControlTemplate.FLAG_MODE_HEAT or
//                        TemperatureControlTemplate.FLAG_MODE_ECO
//                val template = TemperatureControlTemplate(DEV_ID_AT, toggleRangeTemplate,
//                        TemperatureControlTemplate.MODE_COOL,
//                        TemperatureControlTemplate.MODE_COOL, modeFlags)
                getStatefulBuilder(context, controlId)?.setSubtitle(sub)?.setStatusText(status)
                        ?.setControlTemplate(toggleRangeTemplate)
            }
            DEV_ID_MDU -> {
                if (action != null && action is BooleanAction) {
                    // refresh data
                }
                val isRunning = AccessAT.isRunning()
                val status = if (isRunning) "???" else ""
                getStatefulBuilder(context, controlId)?.setStatusText(status)
            }
            else -> null
        }?.build()
    }

}
