package com.madness.collision

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import com.madness.collision.util.X
import java.util.*


internal object Demo {

    fun go(context: Context) {
        if (X.canAccessUsageStats(context)) getForegroundApp(context)
    }

    fun getForegroundApp(context: Context): String {
        val time = System.currentTimeMillis()
        val manager: UsageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val stats: List<UsageStats> = manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 1000, time)
        if (stats.isNullOrEmpty()) return ""
        val mySortedMap: SortedMap<Long, UsageStats> = TreeMap()
        stats.forEach { mySortedMap[it.lastTimeUsed] = it }
        return mySortedMap[mySortedMap.lastKey()]?.packageName ?: ""
    }
}
