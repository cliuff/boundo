package com.madness.collision.qs

import android.annotation.TargetApi
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build
import android.os.Handler
import android.provider.Settings
import android.service.quicksettings.TileService
import android.widget.Toast
import com.madness.collision.R
import com.madness.collision.util.X
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

@TargetApi(Build.VERSION_CODES.N)
internal class TileServiceMonthData : TileService() {
    override fun onTileAdded() {
        TileCommon.inactivate(qsTile)
        Handler().postDelayed({
            ensurePermission()
        }, 10000)
    }

    override fun onStartListening() {
        TileCommon.inactivate(qsTile)
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
        Handler().postDelayed({
            X.toast(applicationContext, R.string.tileDataUsageAccess, Toast.LENGTH_LONG)
        }, 1000)
    }

    private fun update(){
        val qsTile = qsTile ?: return
        val previous = qsTile.label
        qsTile.label = "..."
        qsTile.updateTile()
        var totalGbDay: Double = 0.toDouble()
        var totalGbMonth: Double = 0.toDouble()
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
                val gb: Double = 1073741824.toDouble() // 2^30
                // current day traffic mobile data usage
                val usageDay = manager.querySummaryForDevice(ConnectivityManager.TYPE_MOBILE, null, timeDay, System.currentTimeMillis())
                val receivedDay = usageDay.rxBytes
                val transmittedDay = usageDay.txBytes
                val totalDay = receivedDay + transmittedDay
                totalGbDay = totalDay / gb
                // month traffic mobile data usage
                val usageMonth = manager.querySummaryForDevice(ConnectivityManager.TYPE_MOBILE, null, timeFirstDayOfMonth, System.currentTimeMillis())
                val receivedMonth = usageMonth.rxBytes
                val transmittedMonth = usageMonth.txBytes
                val totalMonth = receivedMonth + transmittedMonth
                totalGbMonth = totalMonth / gb
            }catch (e: Exception){ e.printStackTrace() }
        }.invokeOnCompletion {
            qsTile.label = if (totalGbDay == 0.toDouble() || totalGbMonth == 0.toDouble()) previous else String.format("%.2f•%.2fGB", totalGbDay, totalGbMonth)
            qsTile.updateTile()
        }
    }
}
