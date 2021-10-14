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

package com.madness.collision.unit.api_viewing.device

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import com.jaredrummler.android.device.DeviceName
import com.madness.collision.R
import com.madness.collision.unit.api_viewing.data.VerInfo
import com.madness.collision.unit.api_viewing.databinding.AvDeviceApiBinding
import com.madness.collision.util.CollisionDialog
import com.madness.collision.util.os.OsUtils

internal class DeviceApi {
    fun show(context: Context) {
        val binding = AvDeviceApiBinding.inflate(LayoutInflater.from(context))
        val pop = CollisionDialog(context, R.string.text_alright).apply {
            setCustomContentMere(binding.root)
            setTitleCollision(0, 0, 0)
            setContent(0)
            setListener {
                dismiss()
            }
        }
        val deviceName = Build.MANUFACTURER + " " + DeviceName.getDeviceName()
        val ver = VerInfo(Build.VERSION.SDK_INT)
        binding.run {
            avDeviceName.text = deviceName
            avDeviceApi.text = ver.apiText
        }
        val androidVer = if (ver.api == OsUtils.DEV) "Developer Preview" else ver.sdk
        if (androidVer.isNotEmpty()) {
            val sdk = "Android $androidVer"
            val codeName = ver.codeName(context)
            val sdkDetails = if (codeName != ver.sdk) "$sdk, $codeName" else sdk
            binding.avDeviceSdk.text = sdkDetails
        } else {
            binding.avDeviceSdk.visibility = View.GONE
        }
        pop.show()
    }
}