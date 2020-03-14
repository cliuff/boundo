package com.madness.collision.qs

import android.annotation.TargetApi
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import com.madness.collision.versatile.BarcodeScannerActivity
import com.madness.collision.R
import java.lang.ref.WeakReference

@TargetApi(Build.VERSION_CODES.N)
internal class TileServiceBarcodeScannerMm: TileCommon() {
    companion object {
        lateinit var INSTANCE: WeakReference<TileServiceBarcodeScannerMm>
    }

    override val iconIdle: Icon by lazy { Icon.createWithResource(this, R.drawable.ic_wechat_tile) }
    override val iconBusy: Icon by lazy { Icon.createWithResource(this, R.drawable.ic_logo_fore_vector) }

    override fun onCreate() {
        super.onCreate()
        INSTANCE = WeakReference(this)
    }

    override fun onTileAdded() {
        super.onTileAdded()
        inactivate()
    }

    override fun onStartListening() {
        super.onStartListening()
        inactivate()
    }

    override fun onClick() {
        super.onClick()
        Intent(this, BarcodeScannerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(BarcodeScannerActivity.EXTRA_MODE, BarcodeScannerActivity.MODE_WECHAT)
        }.let { startActivityAndCollapse(it) }
    }
}
