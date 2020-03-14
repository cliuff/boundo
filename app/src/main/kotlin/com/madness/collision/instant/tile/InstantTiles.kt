package com.madness.collision.instant.tile

import com.madness.collision.R
import com.madness.collision.misc.MiscApp
import com.madness.collision.qs.*
import com.madness.collision.unit.Unit

internal object InstantTiles {

    val TILES = listOf(
            instantTile<TileServiceApiViewer>(R.string.apiViewer, Unit.UNIT_NAME_API_VIEWING),
            instantTile<TileServiceAudioTimer>(R.string.unit_audio_timer, Unit.UNIT_NAME_AUDIO_TIMER),
            instantTile<TileServiceBarcodeScannerMm>(R.string.instantTileScannerWechat) { WeChatScannerDesc() }
                    .setRequirement { MiscApp.getPackageInfo(it, packageName = "com.tencent.mm") != null },
            instantTile<TileServiceBarcodeScanner>(R.string.instantTileScannerAlipay) { AlipayScannerDesc() }
                    .setRequirement { MiscApp.getPackageInfo(it, packageName = "com.eg.android.AlipayGphone") != null },
            instantTile<TileServiceMonthData>(R.string.tileData) { MonthDataUsageDesc() }
    )

}
