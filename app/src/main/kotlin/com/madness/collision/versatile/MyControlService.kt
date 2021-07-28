/*
 * Copyright 2021 Clifford Liu
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
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.controls.Control
import android.service.controls.ControlsProviderService
import android.service.controls.DeviceTypes
import android.service.controls.actions.BooleanAction
import android.service.controls.actions.ControlAction
import android.service.controls.actions.FloatAction
import android.service.controls.templates.*
import android.text.format.Formatter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toIcon
import com.madness.collision.R
import com.madness.collision.main.MainActivity
import com.madness.collision.unit.Unit
import com.madness.collision.unit.audio_timer.AccessAT
import com.madness.collision.unit.audio_timer.AtCallback
import com.madness.collision.unit.device_manager.list.DeviceItem
import com.madness.collision.unit.device_manager.list.StateObservable
import com.madness.collision.unit.device_manager.manager.DeviceManager
import com.madness.collision.util.PermissionUtils
import com.madness.collision.util.SysServiceUtils
import com.madness.collision.util.SystemUtil
import com.madness.collision.util.notice.ToastUtils
import com.madness.collision.util.os.OsUtils
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

@TargetApi(OsUtils.R)
class MyControlService : ControlsProviderService(), StateObservable {
    companion object {
        private const val DEV_ID_AT = "dev_at_timer"
        private const val DEV_ID_MDU = "dev_mdu_data"
        private val STATIC_DEVICE_IDS = listOf(DEV_ID_AT, DEV_ID_MDU)
        private const val DEV_PREFIX_DM = "dev_dm"

        private const val ARG_DM_DEV_DEVICE = "dm_dev_device"
    }

    private var updatePublisher: ReplayProcessor<Control>? = null
    private val atMaxValue = 120f
    private val atStepValue = 5f
    private var atCurrentValue = atMaxValue
    private var atCallback: AtCallback? = null
    // block update during timer duration change, which causes temporary shutdown of timer,
    // during which time constant regular state updates are happening,
    // which causes glitches because of rapid switching between states
    private var isAtUpdateBlocked = false
    private var dmManager: DeviceManager? = null
    private var dmSessionNo: Long = 0
    override val stateReceiver: BroadcastReceiver = stateReceiverImp

    override fun onBluetoothStateChanged(state: Int) {
    }

    override fun onDeviceStateChanged(device: BluetoothDevice, state: Int) {
        val updatePublisher = updatePublisher ?: return
        // manager not used yet and no device added
        if (dmSessionNo == 0L) return
        val context = baseContext ?: return
        prepareDmManager(context, dmSessionNo) ?: return
        val id = getDmDeviceIds().find { getDmMacByDeviceId(it) == device.address } ?: return
        val deviceItem = DeviceItem(device).apply { this.state = state }
        val args = mapOf(ARG_DM_DEV_DEVICE to deviceItem)
        getControl(context, id, dmSessionNo, args = args)?.apply {
            updatePublisher.onNext(this)
        }
    }

    private fun getDeviceIds(context: Context, sessionNo: Long): List<String> {
        val dmManager = prepareDmManager(context, sessionNo)
        return if (dmManager != null) STATIC_DEVICE_IDS + getDmDeviceIds()
        else STATIC_DEVICE_IDS
    }

    private fun getDmDeviceIds(): List<String> {
        val dmManager = dmManager ?: return emptyList()
        return dmManager.getPairedDevices().map {
            "${DEV_PREFIX_DM}_${it.address}"
        }
    }

    private fun prepareDmManager(context: Context, sessionNo: Long): DeviceManager? {
        val dmManager = this.dmManager ?: DeviceManager()
        if (this.dmManager == null) this.dmManager = dmManager
        if (dmManager.isDisabled) return null
        if (!dmManager.hasProxy && sessionNo != dmSessionNo) {
            dmSessionNo = sessionNo
            dmManager.initProxy(context, 0)
        }
        return if (dmManager.hasProxy) dmManager else null
    }

    private fun getDmMacByDeviceId(deviceId: String): String {
        val device = "${DEV_PREFIX_DM}_(.*)".toRegex().find(deviceId) ?: return deviceId
        return device.destructured.component1()
    }

    private fun getStatefulBuilder(context: Context, controlId: String): Control.StatefulBuilder? {
        val localeContext = SystemUtil.getLocaleContextSys(context)
        return when(controlId) {
            DEV_ID_AT -> {
                val intent = Intent(context, MainActivity::class.java).apply {
                    putExtras(MainActivity.forItem(Unit.UNIT_NAME_AUDIO_TIMER))
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val pi = PendingIntent.getActivity(context, 0, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT)
                val color = context.getColor(R.color.primaryAWhite)
                val title = localeContext.getString(R.string.unit_audio_timer)
                val iconDrawable = ContextCompat.getDrawable(context, R.drawable.ic_timer_24)
                iconDrawable?.setTint(color)
                val icon = iconDrawable?.toBitmap()?.toIcon()
                Control.StatefulBuilder(DEV_ID_AT, pi)
                        .setTitle(title)
                        .setDeviceType(DeviceTypes.TYPE_GENERIC_START_STOP)
                        .setStatus(Control.STATUS_OK)
                        .setCustomIcon(icon)
            }
            DEV_ID_MDU -> {
                val intent = Intent(context, MainActivity::class.java)
                val pi = PendingIntent.getActivity(context, 0, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT)
                val actionDesc = localeContext.getString(R.string.versatile_device_controls_mdu_ctrl_desc)
                val controlButton = ControlButton(false, actionDesc)
                val color = context.getColor(R.color.primaryAWhite)
                val title = localeContext.getString(R.string.tileData)
                val template = ToggleTemplate(DEV_ID_MDU, controlButton)
                val iconDrawable = ContextCompat.getDrawable(context, R.drawable.ic_data_usage_24)
                iconDrawable?.setTint(color)
                val icon = iconDrawable?.toBitmap()?.toIcon()
                Control.StatefulBuilder(DEV_ID_MDU, pi)
                        .setTitle(title)
                        .setSubtitle(context.getString(R.string.versatile_device_controls_mdu_hint))
                        .setDeviceType(DeviceTypes.TYPE_GENERIC_VIEWSTREAM)
                        .setStatus(Control.STATUS_OK)
                        .setCustomIcon(icon)
                        .setControlTemplate(template)
            }
            else -> if (controlId.startsWith(DEV_PREFIX_DM)) {
                val intent = Intent(context, MainActivity::class.java).apply {
                    putExtras(MainActivity.forItem(Unit.UNIT_NAME_DEVICE_MANAGER))
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val pi = PendingIntent.getActivity(context, 0, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT)
                Control.StatefulBuilder(controlId, pi)
                        .setDeviceType(DeviceTypes.TYPE_GENERIC_ON_OFF)
                        .setStatus(Control.STATUS_OK)
            } else null
        }
    }

    private fun getStatelessBuilder(context: Context, controlId: String): Control.StatelessBuilder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        val context = baseContext ?: return
        registerStateReceiver(context)
    }

    override fun createPublisherForAllAvailable(): Flow.Publisher<Control> {
        val context = baseContext
        val sessionNo = System.currentTimeMillis()
        val controls = getDeviceIds(context, sessionNo).mapNotNull { getControl(context, it, sessionNo) }
        return FlowAdapters.toFlowPublisher(Flowable.fromIterable(controls))
    }

    override fun createPublisherFor(controlIds: MutableList<String>): Flow.Publisher<Control> {
        val context = baseContext
        val updatePublisher = ReplayProcessor.create<Control>().also {
            updatePublisher = it
        }
        val sessionNo = System.currentTimeMillis()
        controlIds.forEach {
            getControl(context, it, sessionNo)?.apply {
                updatePublisher.onNext(this)
            }
        }
        return FlowAdapters.toFlowPublisher(updatePublisher)
    }

    override fun performControlAction(controlId: String, action: ControlAction, consumer: Consumer<Int>) {
        val updatePublisher = updatePublisher ?: return
        val context = baseContext
        // Inform SystemUI that the action has been received and is being processed
        consumer.accept(ControlAction.RESPONSE_OK)
        val sessionNo = System.currentTimeMillis()
        getControl(context, controlId, sessionNo, action)?.apply {
            updatePublisher.onNext(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dmManager?.close()
        val context = baseContext ?: return
        unregisterStateReceiver(context)
    }

    private fun getControl(context: Context, controlId: String, sessionNo: Long, action: ControlAction? = null, args: Map<String, Any?>? = null): Control? {
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
                            if (!isAtUpdateBlocked && doUpdate) updatePublisher?.let {
                                getControl(context, controlId, sessionNo)?.apply {
                                    it.onNext(this)
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
                if (action != null) when(action) {
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
//                    is ModeAction -> {
//                    }
                }
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
//                val modeFlags = TemperatureControlTemplate.FLAG_MODE_COOL or
//                        TemperatureControlTemplate.FLAG_MODE_HEAT or
//                        TemperatureControlTemplate.FLAG_MODE_ECO
//                val template = TemperatureControlTemplate(DEV_ID_AT, toggleRangeTemplate,
//                        TemperatureControlTemplate.MODE_COOL,
//                        TemperatureControlTemplate.MODE_COOL, modeFlags)
                getStatefulBuilder(context, controlId)?.apply {
                    setSubtitle(sub)
                    setStatusText(status)
                    setControlTemplate(toggleRangeTemplate)
                }
            }
            DEV_ID_MDU -> {
                val hasAccess = PermissionUtils.isUsageAccessPermitted(context)
                val doUpdate = action != null && action is BooleanAction
                if (doUpdate && hasAccess) GlobalScope.launch {
                    delay(800)
                    val updatePublisher = updatePublisher ?: return@launch
                    getControl(context, controlId, sessionNo)?.apply {
                        updatePublisher.onNext(this)
                    }
                }
                val status = if (!hasAccess) {
                    localeContext.getString(R.string.text_access_denied)
                } else if (doUpdate) {
                    "..."
                } else {
                    val (totalDay, totalMonth) = SysServiceUtils.getDataUsage(context)
                    val usageDay = Formatter.formatFileSize(context, totalDay)
                    val usageMonth = Formatter.formatFileSize(context, totalMonth)
                    "$usageDay • $usageMonth"
                }
                getStatefulBuilder(context, controlId)?.setStatusText(status)
            }
            else -> if (controlId.startsWith(DEV_PREFIX_DM)) {
                var deviceItem = args?.get(ARG_DM_DEV_DEVICE) as DeviceItem?
                // get device and perform action
                if (deviceItem == null) {
                    val dmManager = prepareDmManager(context, sessionNo)
                    if (dmManager != null) {
                        val mac = getDmMacByDeviceId(controlId)
                        val device = dmManager.getPairedDevices().find { it.address == mac }
                        if (device != null) deviceItem = DeviceItem(device)
                        val isConnected = dmManager.getConnectedDevices().any { it.address == mac }
                        deviceItem?.state = if (isConnected) BluetoothProfile.STATE_CONNECTED
                        else BluetoothProfile.STATE_DISCONNECTED
                        // perform action
                        if (action != null && action is BooleanAction && device != null) {
                            val doConnect = action.newState
                            if (doConnect && !isConnected) {
                                dmManager.connect(device)
                                deviceItem?.state = BluetoothProfile.STATE_CONNECTING
                            } else if (isConnected) {
                                dmManager.disconnect(device)
                                deviceItem?.state = BluetoothProfile.STATE_DISCONNECTING
                            }
                        }
                    } else GlobalScope.launch {
                        // update status in 600ms
                        delay(600)
                        val updatePublisher = updatePublisher ?: return@launch
                        getControl(context, controlId, sessionNo)?.apply {
                            updatePublisher.onNext(this)
                        }
                    }
                }

                val color = context.getColor(R.color.primaryAWhite)
                val iconRes = deviceItem?.iconRes ?: R.drawable.ic_devices_other_24
                val iconDrawable = ContextCompat.getDrawable(context, iconRes)
                iconDrawable?.setTint(color)
                val icon = iconDrawable?.toBitmap()?.toIcon()
                val isConnected = deviceItem?.state == BluetoothProfile.STATE_CONNECTED
                val title = deviceItem?.name ?: localeContext.getString(R.string.versatile_device_controls_dm_title_unknown)
                val subRes = if (isConnected) R.string.versatile_device_controls_dm_hint_on
                else R.string.versatile_device_controls_dm_hint_off
                val sub = localeContext.getString(subRes)
                val status = when (deviceItem?.state) {
                    BluetoothProfile.STATE_CONNECTED -> R.string.dm_list_item_state_connected
                    BluetoothProfile.STATE_CONNECTING -> R.string.dm_list_item_state_connecting
                    BluetoothProfile.STATE_DISCONNECTING -> R.string.dm_list_item_state_disconnecting
                    else -> R.string.dm_list_item_state_disconnected
                }.let { localeContext.getString(it) }
                val actionDescRes = if (isConnected) R.string.versatile_device_controls_dm_ctrl_desc_on
                else R.string.versatile_device_controls_dm_ctrl_desc_off
                val actionDesc = localeContext.getString(actionDescRes)
                val controlButton = ControlButton(isConnected, actionDesc)
                val toggleTemplate = ToggleTemplate(controlId, controlButton)
                getStatefulBuilder(context, controlId)?.apply {
                    setCustomIcon(icon)
                    setTitle(title)
                    setSubtitle(sub)
                    setStatusText(status)
                    setControlTemplate(toggleTemplate)
                }
            } else null
        }?.build()
    }

    private fun getPermission() {
        val intent = Intent().apply {
            action = Settings.ACTION_USAGE_ACCESS_SETTINGS
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        GlobalScope.launch {
            delay(1000)
            ToastUtils.toast(applicationContext, R.string.access_sys_usage, Toast.LENGTH_LONG)
        }
    }

}
