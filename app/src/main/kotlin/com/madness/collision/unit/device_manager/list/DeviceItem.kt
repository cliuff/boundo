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

package com.madness.collision.unit.device_manager.list

import android.bluetooth.BluetoothClass.Device
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import androidx.annotation.DrawableRes
import com.madness.collision.R

internal data class DeviceItem(
    val device: BluetoothDevice,
    val name: String = device.name ?: "",
    val majorDeviceClass: Int = device.bluetoothClass?.majorDeviceClass ?: Device.Major.MISC,
    val deviceClass: Int = device.bluetoothClass?.deviceClass ?: Device.Major.MISC,
    val mac: String = device.address,
    val iconRes: Int = getClassIconRes(deviceClass, majorDeviceClass),
    var state: Int = BluetoothProfile.STATE_DISCONNECTED,
)

@DrawableRes
private fun getClassIconRes(deviceClass: Int, majorClass: Int) = when (majorClass) {
    Device.Major.AUDIO_VIDEO -> when {
        Device.AUDIO_VIDEO_HEADPHONES and deviceClass != 0 -> R.drawable.ic_headphones_24px
        else -> R.drawable.ic_devices_other_24px
    }
    Device.Major.PHONE -> R.drawable.ic_smartphone_24px
    Device.Major.COMPUTER -> R.drawable.ic_computer_24px
    Device.Major.WEARABLE -> when {
        Device.WEARABLE_WRIST_WATCH and deviceClass != 0 -> R.drawable.ic_watch_24px
        else -> R.drawable.ic_devices_wearables_24px
    }
    Device.Major.HEALTH -> R.drawable.ic_health_and_safety_24px
    Device.Major.PERIPHERAL -> when {
        Device.PERIPHERAL_POINTING and deviceClass != 0 -> R.drawable.ic_mouse_24px
        Device.PERIPHERAL_KEYBOARD and deviceClass != 0 -> R.drawable.ic_keyboard_24px
        Device.PERIPHERAL_KEYBOARD_POINTING and deviceClass != 0 -> R.drawable.ic_touchpad_mouse_24px
        else -> R.drawable.ic_devices_other_24px
    }
    else -> R.drawable.ic_devices_other_24px
}
