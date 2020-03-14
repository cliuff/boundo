package com.madness.collision.qs

import android.annotation.TargetApi
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

@TargetApi(Build.VERSION_CODES.N)
internal abstract class TileCommon: TileService(){

    companion object{

        fun inactivate(qsTile: Tile?){
            qsTile ?: return
            qsTile.state = Tile.STATE_INACTIVE
            qsTile.updateTile()
        }
    }

    abstract val iconIdle: Icon
    abstract val iconBusy: Icon

    protected fun inactivate(){
        inactivate(qsTile)
    }
}
