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
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.TileService
import android.text.format.Formatter
import android.widget.Toast
import com.madness.collision.BuildConfig
import com.madness.collision.R
import com.madness.collision.util.SysServiceUtils
import com.madness.collision.util.X
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@TargetApi(Build.VERSION_CODES.N)
internal class TileServiceMonthData : TileService() {
    override fun onTileAdded() {
        TileCommon.inactivate(qsTile)
        GlobalScope.launch {
            delay(10_000)
            ensurePermission()
        }
    }

    private var tileLabel: String = ""
        get() {
            if (field.isEmpty()) {
                field = getString(R.string.tileData)
            }
            return field
        }

    override fun onStartListening() {
        TileCommon.inactivate(qsTile)
        // todo remove fix
        if (BuildConfig.VERSION_CODE > 20080801 && X.aboveOn(X.Q)) {
            qsTile.label = tileLabel
        }
        ensurePermission()
        update()
    }

    override fun onClick() {
        update()
    }

    private fun ensurePermission(){
        if (!X.canAccessUsageStats(this)) getPermission()
    }

    private fun getPermission(){
        startActivityAndCollapse(Intent().apply {
            action = Settings.ACTION_USAGE_ACCESS_SETTINGS
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
        GlobalScope.launch {
            delay(1000)
            X.toast(applicationContext, R.string.access_sys_usage, Toast.LENGTH_LONG)
        }
    }

    private fun update() {
        val context = this
        val qsTile = qsTile ?: return
        val previous = if (X.aboveOn(X.Q)) qsTile.subtitle else qsTile.label
        GlobalScope.launch {
            delay(800)
            val (totalDay, totalMonth) = SysServiceUtils.getDataUsage(context)
            val newVal = if (totalDay == 0L || totalMonth == 0L) previous
            else {
                val usageDay = Formatter.formatFileSize(context, totalDay)
                val usageMonth = Formatter.formatFileSize(context, totalMonth)
                "$usageDay • $usageMonth"
            }
            if (X.aboveOn(X.Q)) {
                qsTile.subtitle = newVal
            } else {
                qsTile.label = newVal
            }
            qsTile.updateTile()
        }
        if (X.aboveOn(X.Q)) {
            qsTile.subtitle = "..."
        } else {
            qsTile.label = "..."
        }
        qsTile.updateTile()
    }
}
