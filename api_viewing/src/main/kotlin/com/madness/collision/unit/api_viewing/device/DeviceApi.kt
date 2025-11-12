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
import androidx.core.view.isVisible
import com.jaredrummler.android.device.DeviceName
import com.madness.collision.R
import com.madness.collision.chief.os.EmuiDistro
import com.madness.collision.chief.os.HyperOsDistro
import com.madness.collision.chief.os.LineageOsDistro
import com.madness.collision.chief.os.MiuiDistro
import com.madness.collision.chief.os.OneUiDistro
import com.madness.collision.chief.os.PreviewBuild
import com.madness.collision.chief.os.UndefDistro
import com.madness.collision.chief.os.distro
import com.madness.collision.unit.api_viewing.data.VerInfo
import com.madness.collision.unit.api_viewing.data.codenameOrNull
import com.madness.collision.unit.api_viewing.data.verNameOrNull
import com.madness.collision.unit.api_viewing.databinding.AvDeviceApiBinding
import com.madness.collision.util.CollisionDialog

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
        val androidPreview = PreviewBuild.codenameOrNull?.let { "$it Preview" }
        val androidVer = androidPreview ?: ver.verNameOrNull?.let { v ->
            listOfNotNull("Android $v", ver.codenameOrNull(context)).joinToString()
        }
        val distro = distro.run {
            val name = displayName
            when (this) {
                is OneUiDistro -> "$name ${oneUI.verName}"
                is EmuiDistro -> "$name API ${emui.apiLevel}"
                is MiuiDistro -> "$name ${miui.displayVersion ?: miui.verName}"
                is HyperOsDistro -> "$name ${hyperOS.displayVersion ?: hyperOS.verName}"
                is LineageOsDistro -> "$name API ${lineageOS.apiLevel}"
                UndefDistro -> null
            }
        }
        val props = listOfNotNull(
            androidVer?.let { "OS: $it" },
            distro?.let { "Distro: $it" },
            getJavaVm()?.let { "Java VM: $it" },
        )
        binding.avDeviceSdk.text = props.joinToString(separator = System.lineSeparator())
        binding.avDeviceSdk.isVisible = props.isNotEmpty()
        pop.show()
    }

    private fun getJavaVm(): String? {
        val vmName = System.getProperty("java.vm.name") ?: return null
        val vmVer = System.getProperty("java.vm.version") ?: return null
        if (vmName.equals("dalvik", true)) {
            val match = """(\d+\.\d+)\.\d+""".toRegex().find(vmVer)
            if (match != null && match.groupValues[1].toFloat() >= 2) return "ART $vmVer"
        }
        return "$vmName $vmVer"
    }
}