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

import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import com.madness.collision.R

internal class DeviceItem(val device: BluetoothDevice) {
    val name: String = device.name
    val majorDeviceClass = device.bluetoothClass.majorDeviceClass
    val deviceClass = device.bluetoothClass.deviceClass
    val mac: String = device.address
    val iconRes: Int = when (majorDeviceClass) {
        BluetoothClass.Device.Major.AUDIO_VIDEO -> R.drawable.ic_devices_other_24
        BluetoothClass.Device.Major.PHONE,
        BluetoothClass.Device.Major.COMPUTER -> R.drawable.ic_devices_24
        BluetoothClass.Device.Major.WEARABLE -> R.drawable.ic_devices_other_24
        BluetoothClass.Device.Major.HEALTH -> R.drawable.ic_heart_24
        BluetoothClass.Device.Major.PERIPHERAL -> R.drawable.ic_devices_other_24
        else -> R.drawable.ic_devices_other_24
    }
    var state: Int = BluetoothProfile.STATE_DISCONNECTED
}
