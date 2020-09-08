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

package com.madness.collision.util

import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import androidx.annotation.RequiresApi
import java.util.*

object SysServiceUtils {
    @RequiresApi(X.M)
    fun getDataUsage(context: Context): Pair<Double, Double> {
        var totalGbDay = 0E0
        var totalGbMonth = 0E0
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val timeDay = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val timeFirstDayOfMonth = cal.timeInMillis
        val manager = context.getSystemService(Context.NETWORK_STATS_SERVICE)
                as NetworkStatsManager? ?: return 0E0 to 0E0
        val gb = 1073741824E0 // 2^30
        val time = System.currentTimeMillis()
        try {
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return totalGbDay to totalGbMonth
    }
}
