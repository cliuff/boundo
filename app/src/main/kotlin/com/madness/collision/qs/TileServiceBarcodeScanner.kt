/*
 * Copyright 2020 Clifford Liu
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

package com.madness.collision.qs

import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import com.madness.collision.util.os.OsUtils
import com.madness.collision.versatile.BarcodeScannerActivity
import com.madness.collision.R
import java.lang.ref.WeakReference

@TargetApi(Build.VERSION_CODES.N)
internal class TileServiceBarcodeScanner: TileCommon() {
    companion object {
        lateinit var INSTANCE: WeakReference<TileServiceBarcodeScanner>
    }

    override val iconIdle: Icon by lazy { Icon.createWithResource(this, R.drawable.ic_alipay_tile) }
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
        val intent = Intent(this, BarcodeScannerActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            .putExtra(BarcodeScannerActivity.EXTRA_MODE, BarcodeScannerActivity.MODE_ALIPAY)
        if (OsUtils.satisfy(OsUtils.U)) {
            val pdIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            startActivityAndCollapse(pdIntent)
        } else {
            startActivityAndCollapse(intent)
        }
    }
}
