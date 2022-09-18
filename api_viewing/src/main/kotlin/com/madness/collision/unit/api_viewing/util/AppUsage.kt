/*
 * Copyright 2022 Clifford Liu
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

package com.madness.collision.unit.api_viewing.util

import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import androidx.core.content.getSystemService

object AppUsage {

    // requires PACKAGE_USAGE_STATS permission
    fun getUsed(context: Context, beginTime: Long, endTime: Long): List<String> {
        val manager = context.getSystemService<UsageStatsManager>() ?: return emptyList()
        val stats = manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, beginTime, endTime)
        if (stats.isNullOrEmpty()) return emptyList()
        val st = stats.filter { it.lastTimeUsed > 0 }.sortedByDescending { it.lastTimeUsed }
        val s = st.joinToString(prefix = "Apps:\n", separator = "\n") { "${it.packageName}:${it.lastTimeUsed}" }
        Log.d("test.usage", s)
        return st.map { it.packageName }
    }
}