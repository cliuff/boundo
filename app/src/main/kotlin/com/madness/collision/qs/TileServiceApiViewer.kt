package com.madness.collision.qs

import android.annotation.TargetApi
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import com.madness.collision.main.MainActivity
import com.madness.collision.unit.Unit

@TargetApi(Build.VERSION_CODES.N)
class TileServiceApiViewer : TileService() {
    override fun onTileAdded() {
        TileCommon.inactivate(qsTile)
    }

    override fun onStartListening() {
        TileCommon.inactivate(qsTile)
    }

    override fun onClick() {
        Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtras(MainActivity.forItem(Unit.UNIT_NAME_API_VIEWING))
            startActivityAndCollapse(this)
        }
    }
}
