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
import com.madness.collision.unit.api_viewing.data.ApiUnit
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.origin.AppRetriever
import com.madness.collision.unit.api_viewing.origin.OriginRetriever
import com.madness.collision.unit.api_viewing.origin.PackageRetriever
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * [OriginRetriever] proxy to update database record
 */
internal class RecordMaintainer<T>(private val context: Context,
                                   private val target: OriginRetriever<T>,
                                   private val scope: CoroutineScope,
                                   private val dao: AppDao = DataMaintainer.get(context, scope))
    : OriginRetriever<T> {

    companion object {

        fun pack(context: Context, scope: CoroutineScope): RecordMaintainer<PackageInfo> {
            return RecordMaintainer(context, PackageRetriever(context), scope)
        }

        fun app(context: Context, scope: CoroutineScope): RecordMaintainer<ApiViewingApp> {
            return RecordMaintainer(context, AppRetriever(context), scope)
        }
    }

    /**
     * Update records when getting any packages,
     * this method is the base and always gets invoked
     */
    override fun get(predicate: ((PackageInfo) -> Boolean)?): List<T> {
        return target.get(predicate).also {
            // update asynchronously
            scope.launch(Dispatchers.Default) {
                update(it)
            }
        }
    }

    /**
     * Check removal when getting all packages,
     * this is an extra after the invocation of [get] by predicate
     */
    override fun get(unit: Int): List<T> {
        return super.get(unit).also {
            // update asynchronously
            if (unit != ApiUnit.ALL_APPS) return@also
            scope.launch(Dispatchers.Default) {
                checkRemoval(it)
            }
        }
    }

    /**
     * Check removal when getting all packages,
     * this is an extra after the invocation of [get] by predicate
     */
    override fun get(): List<T> {
        return super.get().also {
            // update asynchronously
            scope.launch(Dispatchers.Default) {
                checkRemoval(it)
            }
        }
    }

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
