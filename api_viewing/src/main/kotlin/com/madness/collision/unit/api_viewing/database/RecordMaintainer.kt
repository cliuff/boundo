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

package com.madness.collision.unit.api_viewing.database

import android.content.Context
import android.content.pm.PackageInfo
import com.madness.collision.unit.api_viewing.data.ApiViewingApp

/**
 * update database record
 */
internal class RecordMaintainer<T>(private val context: Context, private val dao: AppDao) {
    /**
     * Add and update
     */
    fun update(list: List<T>) {
        val getApp: (T) -> ApiViewingApp? = {
            when (it) {
                is PackageInfo -> ApiViewingApp(context, it, preloadProcess = true, archive = false)
                is ApiViewingApp -> it
                else -> null
            }
        }
        val apps = if ((dao.selectCount() ?: 0) == 0) {
            list.mapNotNull(getApp)
        } else list.mapNotNull {
            val (packageName, updateTime) = when (it) {
                is PackageInfo -> it.packageName to it.lastUpdateTime
                is ApiViewingApp -> it.packageName to it.updateTime
                else -> return@mapNotNull null
            }
            val recordedUpdateTime = dao.selectUpdateTime(packageName) ?: 0
            if (updateTime <= recordedUpdateTime) return@mapNotNull null
            getApp(it)
        }
        dao.insert(apps)
    }

    /**
     * Remove
     */
    fun checkRemoval(list: List<T>) {
        if ((dao.selectCount() ?: 0) == 0) return
        list.mapNotNull {
            when (it) {
                is PackageInfo -> it.packageName
                is ApiViewingApp -> it.packageName
                else -> null
            }
        }.let {
            dao.deleteNonExistPackageNames(it)
        }
    }
}
