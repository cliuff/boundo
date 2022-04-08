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

import android.Manifest
import android.annotation.TargetApi
import android.app.PendingIntent
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.controls.Control
import android.service.controls.DeviceTypes
import android.service.controls.actions.BooleanAction
import android.service.controls.actions.ControlAction
import android.service.controls.templates.ControlButton
import android.service.controls.templates.ToggleTemplate
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toIcon
import com.madness.collision.R
import com.madness.collision.main.MainActivity
import com.madness.collision.unit.Unit
import com.madness.collision.unit.device_manager.list.DeviceItem
import com.madness.collision.unit.device_manager.list.StateObservable
import com.madness.collision.unit.device_manager.list.StateUpdateRegulation
import com.madness.collision.unit.device_manager.list.StateUpdateRegulator
import com.madness.collision.unit.device_manager.manager.DeviceManager
import com.madness.collision.util.PermissionUtils
import com.madness.collision.util.SystemUtil
import com.madness.collision.util.os.OsUtils
import com.madness.collision.util.regexOf
import io.reactivex.rxjava3.processors.ReplayProcessor
import kotlinx.coroutines.*

@TargetApi(Build.VERSION_CODES.R)
class DeviceManControlProvider(private val context: Context) : ControlProvider, StateObservable {
    override val controlIdRegex: String = regexOf("${DEV_PREFIX_DM}_.+")
    private val coroutineScope = CoroutineScope(Dispatchers.Default + Job())
    private val devManLazy = lazy { DeviceManager() }
    private val devMan by devManLazy
    private val devManValue: DeviceManager? get() = if (devManLazy.isInitialized()) devManLazy.value else null
    private val dmRegulator: StateUpdateRegulator by lazy { StateUpdateRegulator() }
    private var updatePublisher: ReplayProcessor<Control>? = null

    companion object {
        const val DEV_PREFIX_DM = "dev_dm"
    }

    override val stateReceiver: BroadcastReceiver = stateReceiverImp

    override fun onBluetoothStateChanged(state: Int) {
    }

    override fun onDeviceStateChanged(device: BluetoothDevice, state: Int) {
        val updatePublisher = updatePublisher ?: return
        // manager not used yet and no device added
        if (devMan.hasProxy.not()) return
        prepareDmManager(context) ?: return
        val id = getDmDeviceIds().find { getDmMacByDeviceId(it) == device.address } ?: return
        val deviceItem = DeviceItem(device).apply { this.state = state }
        val regulation = StateUpdateRegulation(600, deviceItem) {
            withContext(Dispatchers.Default) {
                val control = getStatefulControl(context, id, deviceItem)
                updatePublisher.onNext(control)
            }
        }
        coroutineScope.launch { dmRegulator.regulate(regulation) }
    }

    override fun onCreate() {
        registerStateReceiver(context)
    }

    override fun onDestroy() {
        coroutineScope.cancel()
        devManValue?.close()
        unregisterStateReceiver(context)
    }

    override suspend fun getDeviceIds(): List<String> {
        prepareDevMan(context) ?: return emptyList()
        return getDmDeviceIds()
    }

    override suspend fun getStatelessControl(controlId: String): Control? {
        val devMan = prepareDevMan(context) ?: return null
        val deviceItem = getDeviceItem(devMan, controlId, null)
        return getStatelessControl(context, controlId, deviceItem)
    }

    override fun getStatefulControl(updatePublisher: ReplayProcessor<Control>, controlId: String, action: ControlAction?) {
        this.updatePublisher = updatePublisher
        coroutineScope.launch {
            val devMan = prepareDevMan(context)
            val deviceItem = devMan?.let { getDeviceItem(it, controlId, action) }
            val control = getStatefulControl(context, controlId, deviceItem)
            updatePublisher.onNext(control)
        }
    }

    private fun getStatelessControl(context: Context, controlId: String, deviceItem: DeviceItem?): Control {
        val localeContext = SystemUtil.getLocaleContextSys(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtras(MainActivity.forItem(Unit.UNIT_NAME_DEVICE_MANAGER))
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val piMutabilityFlag = if (OsUtils.satisfy(OsUtils.M)) PendingIntent.FLAG_IMMUTABLE else 0
        val pi = PendingIntent.getActivity(context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or piMutabilityFlag)
        val iconRes = deviceItem?.iconRes ?: R.drawable.ic_devices_other_24
        val iconDrawable = ContextCompat.getDrawable(context, iconRes)
        val color = context.getColor(R.color.primaryAWhite)
        iconDrawable?.setTint(color)
        val icon = iconDrawable?.toBitmap()?.toIcon()
        val title = deviceItem?.name ?: localeContext.getString(R.string.versatile_device_controls_dm_title_unknown)
        val isConnected = deviceItem?.state == BluetoothProfile.STATE_CONNECTED
        val subRes = if (isConnected) R.string.versatile_device_controls_dm_hint_on
        else R.string.versatile_device_controls_dm_hint_off
        val sub = localeContext.getString(subRes)
        return Control.StatelessBuilder(controlId, pi)
            .setCustomIcon(icon)
            .setTitle(title)
            .setSubtitle(sub)
            .setDeviceType(DeviceTypes.TYPE_GENERIC_ON_OFF)
            .build()
    }

    private fun getStatefulControl(context: Context, controlId: String, deviceItem: DeviceItem?): Control {
        val localeContext = SystemUtil.getLocaleContextSys(context)
        val isConnected = deviceItem?.state == BluetoothProfile.STATE_CONNECTED
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
        val stateless = getStatelessControl(context, controlId, deviceItem)
        return Control.StatefulBuilder(stateless)
            .setStatus(Control.STATUS_OK)
            .setStatusText(status)
            .setControlTemplate(toggleTemplate)
            .build()
    }

    private fun getDmDeviceIds(): List<String> {
        return devMan.getPairedDevices().map { "${DEV_PREFIX_DM}_${it.address}" }
    }

    private suspend fun prepareDevMan(context: Context): DeviceManager? {
        val init = prepareDevManActual(context)
        if (init == true) devMan.initProxy(context)
        return if (devMan.hasProxy) devMan else null
    }

    private fun prepareDmManager(context: Context): DeviceManager? {
        val init = prepareDevManActual(context)
        // the invocation contains asynchronous call to service connection,
        // but it is impossible to wait for service connection callback,
        // which will be invoked after code execution of the call site,
        // because both of them are executed on the Main Thread, thus invoked sequentially
        if (init == true) devMan.initProxySync(context)
        return if (devMan.hasProxy) devMan else null
    }

    private fun prepareDevManActual(context: Context): Boolean? {
        val dmManager = devMan
        if (dmManager.isDisabled) return null
        // check runtime bluetooth permission to get paired devices
        if (OsUtils.satisfy(OsUtils.S)) {
            val permission = Manifest.permission.BLUETOOTH_CONNECT
            if (PermissionUtils.check(context, arrayOf(permission)).isNotEmpty()) return null
        }
        return dmManager.hasProxy.not()
    }

    private fun getDmMacByDeviceId(deviceId: String): String {
        val device = "${DEV_PREFIX_DM}_(.*)".toRegex().find(deviceId) ?: return deviceId
        return device.destructured.component1()
    }

    private fun getDeviceItem(dmManager: DeviceManager, controlId: String, action: ControlAction?): DeviceItem? {
        val mac = getDmMacByDeviceId(controlId)
        val device = dmManager.getPairedDevices().find { it.address == mac }
        val deviceItem = if (device != null) DeviceItem(device) else null
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
        return deviceItem
    }
}