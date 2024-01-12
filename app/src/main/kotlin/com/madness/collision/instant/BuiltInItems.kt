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

package com.madness.collision.instant

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.madness.collision.R
import com.madness.collision.instant.tile.AlipayScannerDesc
import com.madness.collision.instant.tile.MonthDataUsageDesc
import com.madness.collision.instant.tile.WeChatScannerDesc
import com.madness.collision.misc.MiscApp
import com.madness.collision.qs.TileServiceApiViewer
import com.madness.collision.qs.TileServiceAudioTimer
import com.madness.collision.qs.TileServiceBarcodeScanner
import com.madness.collision.qs.TileServiceBarcodeScannerMm
import com.madness.collision.qs.TileServiceMonthData
import com.madness.collision.settings.DeviceControlsFragment
import com.madness.collision.unit.Unit
import com.madness.collision.util.P
import com.madness.collision.versatile.MyControlService
import com.madness.collision.versatile.TextProcessingActivity

object BuiltInItems {
    val Shortcuts = listOf(
        InstantShortcut(P.SC_ID_API_VIEWER, R.string.apiViewer, Unit.UNIT_NAME_API_VIEWING),
        InstantShortcut(P.SC_ID_AUDIO_TIMER, R.string.unit_audio_timer, Unit.UNIT_NAME_AUDIO_TIMER),
        InstantShortcut(P.SC_ID_DEVICE_MANAGER, R.string.unit_device_manager, Unit.UNIT_NAME_DEVICE_MANAGER),
    )
    // tile service classes need to be loaded on Android N+
    @get:RequiresApi(Build.VERSION_CODES.N)
    val Tiles by lazy(LazyThreadSafetyMode.NONE) {
        listOf(
            comp<TileServiceApiViewer>(R.string.apiViewer, Unit.UNIT_NAME_API_VIEWING),
            comp<TileServiceAudioTimer>(R.string.unit_audio_timer, Unit.UNIT_NAME_AUDIO_TIMER),
            comp<TileServiceBarcodeScannerMm>(R.string.instantTileScannerWechat) { WeChatScannerDesc() }
                .setRequirement { MiscApp.isAppAvailable(it, "com.tencent.mm", "instant.tile" to "WeChat not installed") },
            comp<TileServiceBarcodeScanner>(R.string.instantTileScannerAlipay) { AlipayScannerDesc() }
                .setRequirement { MiscApp.isAppAvailable(it, "com.eg.android.AlipayGphone", "instant.tile" to "Alipay not installed") },
            comp<TileServiceMonthData>(R.string.tileData) { MonthDataUsageDesc() }
        )
    }
    val Others = listOf(
        // Classes of higher API level cannot be loaded on lower API level
        Build.VERSION_CODES.M to { comp<TextProcessingActivity>(R.string.activityTextProcessingApp, Unit.UNIT_NAME_API_VIEWING) },
        Build.VERSION_CODES.R to { comp<MyControlService>(R.string.app_device_controls) { DeviceControlsFragment() } }
    )
}

private inline fun <reified T: Any> comp(
    displayNameResId: Int,
    requiredUnitName: String = "",
    noinline descriptionPageGetter: (() -> Fragment)? = null
): InstantComponent<T> {
    return InstantComponent(displayNameResId, T::class, requiredUnitName, descriptionPageGetter)
}
