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

package com.madness.collision.instant.tile

import com.madness.collision.R
import com.madness.collision.instant.instantComponent
import com.madness.collision.misc.MiscApp
import com.madness.collision.qs.*
import com.madness.collision.unit.Unit

internal object InstantTiles {

    val TILES = listOf(
            instantComponent<TileServiceApiViewer>(R.string.apiViewer, Unit.UNIT_NAME_API_VIEWING),
            instantComponent<TileServiceAudioTimer>(R.string.unit_audio_timer, Unit.UNIT_NAME_AUDIO_TIMER),
            instantComponent<TileServiceBarcodeScannerMm>(R.string.instantTileScannerWechat) { WeChatScannerDesc() }
                    .setRequirement { MiscApp.isAppAvailable(it, "com.tencent.mm",
                            "instant.tile" to "WeChat not installed") },
            instantComponent<TileServiceBarcodeScanner>(R.string.instantTileScannerAlipay) { AlipayScannerDesc() }
                    .setRequirement { MiscApp.isAppAvailable(it, "com.eg.android.AlipayGphone",
                            "instant.tile" to "Alipay not installed") },
            instantComponent<TileServiceMonthData>(R.string.tileData) { MonthDataUsageDesc() }
    )

}
