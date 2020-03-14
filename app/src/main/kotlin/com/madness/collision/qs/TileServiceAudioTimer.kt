package com.madness.collision.qs

import android.annotation.TargetApi
import android.os.Build
import android.service.quicksettings.TileService
import com.madness.collision.unit.audio_timer.AccessAT

@TargetApi(Build.VERSION_CODES.N)
class TileServiceAudioTimer : TileService() {
    override fun onTileAdded() {
        TileCommon.inactivate(qsTile)
    }

    override fun onStartListening() {
        TileCommon.inactivate(qsTile)
    }

    override fun onClick() {
        AccessAT.start(this)
    }

}
