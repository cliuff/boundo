/*
 * Copyright 2024 Clifford Liu
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

package com.madness.collision.unit.api_viewing.apps

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.util.P

interface UpdateRepository {
    fun getPkgChangedTime(): Long
    fun getChangedPackages(context: Context, isFreshInstall: Boolean): Pair<AppPkgChanges, Long>
    fun getMaintainedApp(): ApiViewingApp
}

class UpdateRepoImpl(
    private val appRepo: AppRepository,
    private val settingsPrefs: SharedPreferences,
) : UpdateRepository {
    // timestamp to retrieve app updates, set the first retrieval
    private var lastAppTimestamp: Long = -1L

    override fun getPkgChangedTime(): Long {
        return settingsPrefs.getLong(P.PACKAGE_CHANGED_TIMESTAMP, -1)
    }

    override fun getChangedPackages(context: Context, isFreshInstall: Boolean): Pair<AppPkgChanges, Long> {
        return if (lastAppTimestamp == -1L) {
            val lastTimestamp = when {
                // display recent updates in last week if no history (by default)
                isFreshInstall -> System.currentTimeMillis() - 604_800_000
                else -> settingsPrefs.getLong(P.PACKAGE_CHANGED_TIMESTAMP, -1)
            }
            val currentTime = System.currentTimeMillis()
            // use last persisted timestamp to retrieve updates then persist current time
            val result = appRepo.getChangedPackages(context, lastTimestamp)
            settingsPrefs.edit { putLong(P.PACKAGE_CHANGED_TIMESTAMP, currentTime) }
            lastAppTimestamp = lastTimestamp
            result to currentTime
        } else {
            val currentTime = System.currentTimeMillis()
            // use last the same timestamp used the last time to retrieve updates
            appRepo.getChangedPackages(context, lastAppTimestamp) to currentTime
        }
    }

    override fun getMaintainedApp(): ApiViewingApp {
        return appRepo.getMaintainedApp()
    }
}
