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
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.TileService
import android.widget.Toast
import com.madness.collision.BuildConfig
import com.madness.collision.R
import com.madness.collision.util.X
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

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
            X.toast(applicationContext, R.string.tileDataUsageAccess, Toast.LENGTH_LONG)
        }
    }

    private fun update(){
        val qsTile = qsTile ?: return
        val previous = if (X.aboveOn(X.Q)) qsTile.subtitle else qsTile.label
        if (X.aboveOn(X.Q)) {
            qsTile.subtitle = "..."
        } else {
            qsTile.label = "..."
        }
        qsTile.updateTile()
        var totalGbDay = 0E0
        var totalGbMonth = 0E0
        GlobalScope.launch {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val timeDay = cal.timeInMillis
            cal.set(Calendar.DAY_OF_MONTH, 1)
            val timeFirstDayOfMonth = cal.timeInMillis
            val manager = getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager? ?: return@launch
            try {
                val gb = 1073741824E0 // 2^30
                val time = System.currentTimeMillis()
                // current day traffic mobile data usage
                val usageDay = manager.querySummaryForDevice(
                        ConnectivityManager.TYPE_MOBILE, null, timeDay, time)
                val receivedDay = usageDay.rxBytes
                val transmittedDay = usageDay.txBytes
                val totalDay = receivedDay + transmittedDay
                totalGbDay = totalDay / gb
                // month traffic mobile data usage
                val usageMonth = manager.querySummaryForDevice(
                        ConnectivityManager.TYPE_MOBILE, null, timeFirstDayOfMonth, time)
                val receivedMonth = usageMonth.rxBytes
                val transmittedMonth = usageMonth.txBytes
                val totalMonth = receivedMonth + transmittedMonth
                totalGbMonth = totalMonth / gb
            } catch (e: Exception){ e.printStackTrace() }
        }.invokeOnCompletion {
            val newVal = if (totalGbDay == 0E0 || totalGbMonth == 0E0) previous
            else String.format("%.2f • %.2f GB", totalGbDay, totalGbMonth)
            if (X.aboveOn(X.Q)) {
                qsTile.subtitle = newVal
            } else {
                qsTile.label = newVal
            }
            qsTile.updateTile()
        }
    }
}
