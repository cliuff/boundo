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

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.os.Build
import android.service.controls.actions.BooleanAction
import android.service.controls.actions.ControlAction
import androidx.annotation.RequiresApi
import com.madness.collision.R
import com.madness.collision.unit.device_manager.list.DeviceItem
import com.madness.collision.unit.device_manager.list.StateObservable
import com.madness.collision.unit.device_manager.list.StateUpdateRegulation
import com.madness.collision.unit.device_manager.list.StateUpdateRegulator
import com.madness.collision.unit.device_manager.manager.DeviceManager
import com.madness.collision.util.PermissionUtils
import com.madness.collision.util.SystemUtil
import com.madness.collision.util.os.OsUtils
import com.madness.collision.util.regexOf
import com.madness.collision.versatile.controls.CommonControl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

typealias ControlStateChange = Pair<BluetoothDevice, Int>

@RequiresApi(Build.VERSION_CODES.R)
class DevManControlCreator : ControlCreator<ControlInfo>, StateObservable {
    companion object {
        const val DEV_PREFIX_DM = "dev_dm"
        val IdRegex = regexOf("${DEV_PREFIX_DM}_.+")
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Default + Job())
    private val devManLazy = lazy { DeviceManager() }
    private val devMan by devManLazy
    private val dmRegulator: StateUpdateRegulator by lazy { StateUpdateRegulator() }
    private var stateChannel: SendChannel<ControlStateChange>? = null
    private lateinit var stateFlow: Flow<ControlStateChange>

    override val stateReceiver: BroadcastReceiver = stateReceiverImp

    override fun onBluetoothStateChanged(state: Int) {
    }

    override fun onDeviceStateChanged(device: BluetoothDevice, state: Int) {
        coroutineScope.launch { stateChannel?.send(device to state) }
    }

    fun onCreate(context: Context) {
        val flow = channelFlow {
            stateChannel = channel
            awaitClose { stateChannel = null }
        }
        // SharedFlow/hot flow required for multiple subscribers to be allowed
        stateFlow = flow.shareIn(coroutineScope, SharingStarted.WhileSubscribed())
        registerStateReceiver(context)
    }

    fun close(context: Context) {
        if (devManLazy.isInitialized()) devMan.close()
        stateChannel?.close()
        unregisterStateReceiver(context)
    }

    suspend fun getDeviceIds(context: Context): List<String> {
        prepareDevMan(context) ?: return emptyList()
        return getDmDeviceIds()
    }

    override suspend fun create(context: Context, id: String): ControlInfo? {
        val devMan = prepareDevMan(context) ?: return null
        val deviceItem = getDeviceItem(devMan, id, null)
        return getDeviceDetails(context, deviceItem)
    }

    override fun create(context: Context, id: String, actionFlow: Flow<ControlActionRequest>): Flow<ControlInfo> {
        return channelFlow {
            prepareDevMan(context)?.let { devMan ->
                val deviceItem = getDeviceItem(devMan, id, null)
                send(getStatus(context, deviceItem))
            }
            actionFlow
                .onEach { (_, action) ->
                    val devMan = prepareDevMan(context)
                    val deviceItem = devMan?.let { getDeviceItem(it, id, action) }
                    send(getStatus(context, deviceItem))
                }
                .launchIn(this)
            val producerScope = this
            stateFlow
                .filter { (device, _) ->
                    getDmDeviceIds().find { getDmMacByDeviceId(it) == device.address } == id
                }
                .onEach state@{ (device, state) ->
                    // manager not used yet and no device added
                    if (devMan.hasProxy.not()) return@state
                    prepareDmManager(context) ?: return@state
                    val deviceItem = DeviceItem(device).apply { this.state = state }
                    val regulation = StateUpdateRegulation(600, deviceItem) {
                        send(getStatus(context, deviceItem))
                    }
                    // launch asynchronously to avoid blocking flow item processing
                    producerScope.launch { dmRegulator.regulate(regulation) }
                }
                .launchIn(this)
        }
    }

    private fun getDeviceDetails(context: Context, deviceItem: DeviceItem?): ControlDetails =
        CommonControl(context) {
            val localeContext = SystemUtil.getLocaleContextSys(context)
            if (deviceItem != null) {
                val subRes = when (deviceItem.state) {
                    BluetoothProfile.STATE_CONNECTED -> R.string.versatile_device_controls_dm_hint_on
                    else -> R.string.versatile_device_controls_dm_hint_off
                }
                ControlDetails(
                    title = deviceItem.name,
                    subtitle = localeContext.getString(subRes),
                    icon = drawableIcon(deviceItem.iconRes),
                )
            } else {
                ControlDetails(
                    title = localeContext.getString(R.string.versatile_device_controls_dm_title_unknown),
                    subtitle = localeContext.getString(R.string.versatile_device_controls_dm_hint_off),
                    icon = drawableIcon(R.drawable.ic_devices_other_24),
                )
            }
        }

    private fun getStatus(context: Context, deviceItem: DeviceItem?): ControlInfo.ButtonStatus {
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
        val details = getDeviceDetails(context, deviceItem)
        return ControlInfo.ButtonStatus(details, status, isConnected, actionDesc)
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