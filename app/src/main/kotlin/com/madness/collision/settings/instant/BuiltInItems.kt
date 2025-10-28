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

package com.madness.collision.settings.instant

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.madness.collision.R
import com.madness.collision.settings.instant.tile.AlipayScannerDesc
import com.madness.collision.settings.instant.tile.MonthDataUsageDesc
import com.madness.collision.settings.instant.tile.WeChatScannerDesc
import com.madness.collision.misc.MiscApp
import com.madness.collision.qs.TileServiceAudioTimer
import com.madness.collision.qs.TileServiceBarcodeScanner
import com.madness.collision.qs.TileServiceBarcodeScannerMm
import com.madness.collision.qs.TileServiceMonthData
import com.madness.collision.unit.Unit
import com.madness.collision.util.P

object BuiltInItems {
    val Shortcuts = listOf(
        InstantShortcut(P.SC_ID_AUDIO_TIMER, R.string.unit_audio_timer, Unit.UNIT_NAME_AUDIO_TIMER),
    )
    // tile service classes need to be loaded on Android N+
    @get:RequiresApi(Build.VERSION_CODES.N)
    val Tiles by lazy(LazyThreadSafetyMode.NONE) {
        listOf(
            comp<TileServiceAudioTimer>(R.string.unit_audio_timer, Unit.UNIT_NAME_AUDIO_TIMER),
            comp<TileServiceBarcodeScannerMm>(R.string.instantTileScannerWechat) { WeChatScannerDesc() }
                .setRequirement { MiscApp.isAppAvailable(it, "com.tencent.mm", "instant.tile" to "WeChat not installed") },
            comp<TileServiceBarcodeScanner>(R.string.instantTileScannerAlipay) { AlipayScannerDesc() }
                .setRequirement { MiscApp.isAppAvailable(it, "com.eg.android.AlipayGphone", "instant.tile" to "Alipay not installed") },
            comp<TileServiceMonthData>(R.string.tileData) { MonthDataUsageDesc() }
        )
    }
    val Others = emptyList<Pair<Int, () -> InstantComponent<*>>>()
}

private inline fun <reified T: Any> comp(
    displayNameResId: Int,
    requiredUnitName: String = "",
    noinline descriptionPageGetter: (() -> Fragment)? = null
): InstantComponent<T> {
    return InstantComponent(displayNameResId, T::class, requiredUnitName, descriptionPageGetter)
}
