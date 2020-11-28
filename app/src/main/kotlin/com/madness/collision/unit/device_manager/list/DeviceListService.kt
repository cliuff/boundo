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

package com.madness.collision.unit.device_manager.list

import android.bluetooth.BluetoothProfile
import com.madness.collision.unit.device_manager.manager.DeviceManager

internal class DeviceListService(private val manager: DeviceManager) {

    fun getDeviceItems(): List<DeviceItem> {
        val pairedDevices = manager.getPairedDevices()
        if (pairedDevices.isEmpty()) return emptyList()
        val connectedDevices = manager.getConnectedDevices()
        return pairedDevices.map { device ->
            DeviceItem(device).apply {
                state = if (device in connectedDevices) BluetoothProfile.STATE_CONNECTED
                else BluetoothProfile.STATE_DISCONNECTED
            }
        }
    }
}