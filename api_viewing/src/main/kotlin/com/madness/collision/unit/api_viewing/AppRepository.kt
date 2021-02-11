/*
 * Copyright 2021 Clifford Liu
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

package com.madness.collision.unit.api_viewing

import android.content.Context
import androidx.annotation.WorkerThread
import com.madness.collision.unit.api_viewing.data.ApiUnit
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.database.AppDao
import com.madness.collision.unit.api_viewing.origin.AppRetriever

internal class AppRepository(private val dao: AppDao) {

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun insert(app: ApiViewingApp) {
        dao.insert(app)
    }

    fun getAllApps(context: Context): List<ApiViewingApp> = getApps(context, ApiUnit.ALL_APPS)

    fun getUserApps(context: Context): List<ApiViewingApp> = getApps(context, ApiUnit.USER)

    fun getSystemApps(context: Context): List<ApiViewingApp> = getApps(context, ApiUnit.SYS)

    fun getApps(context: Context, unit: Int): List<ApiViewingApp> {
        if ((dao.selectCount() ?: 0) == 0) {
            val apps = AppRetriever(context).all
            dao.insert(apps)
            return when (unit) {
                ApiUnit.USER, ApiUnit.SYS -> apps.filter { it.apiUnit == unit }
                else -> apps
            }
        }
        return dao.selectApps(unit)
    }
}
