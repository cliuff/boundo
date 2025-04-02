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
import com.madness.collision.unit.api_viewing.data.ApiUnit
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.data.toEntities
import com.madness.collision.unit.api_viewing.data.toEntity
import com.madness.collision.unit.api_viewing.data.toEntityApp
import com.madness.collision.unit.api_viewing.data.toEntityApps
import com.madness.collision.unit.api_viewing.data.toFullApps
import com.madness.collision.unit.api_viewing.database.AppDao
import com.madness.collision.unit.api_viewing.database.AppEntity
import kotlinx.coroutines.runBlocking

/** Mediator that is responsible for converting between [ApiViewingApp] and [AppEntity] */
class AppMediatorRepo(private val appDao: AppDao, private val context: Context) {
    fun add(vararg app: ApiViewingApp) {
        when {
            app.size == 1 -> appDao.insert(app[0].toEntity())
            else -> appDao.insert(app.map { it.toEntity() })
        }
    }

    fun add(apps: List<ApiViewingApp>) {
        appDao.insert(apps.toEntities())
    }

    fun get(packageName: String, init: Boolean = true): ApiViewingApp? {
        val ent = appDao.selectApp(packageName) ?: return null
        return ent.toEntityApp(getMaintainedApp()).apply { if (init) initIgnored(context) }
    }

    private fun List<AppEntity>.toApps(init: Boolean): List<ApiViewingApp> {
        if (isEmpty()) return emptyList()
        if (!init) return toEntityApps(getMaintainedApp())
        return runBlocking { toFullApps(getMaintainedApp(), context) }
    }

    /** Get apps by [packageNames], the result list retains the original order. */
    fun get(packageNames: List<String>, init: Boolean = true): List<ApiViewingApp> {
        val entities = appDao.selectApps(packageNames).associateBy(AppEntity::packageName)
        return packageNames.mapNotNull(entities::get).toApps(init)
    }

    fun get(unit: Int, init: Boolean = true): List<ApiViewingApp> =
        appDao.selectApps(unit).toApps(init)

    fun getAll(init: Boolean = true): List<ApiViewingApp> =
        get(ApiUnit.ALL_APPS, init = init)

    fun getMaintainedApp(): ApiViewingApp = MaintainedApp(::add)
}

/** [ApiViewingApp] proxy to update database record. */
internal class MaintainedApp(private val onRetrieved: MaintainedApp.() -> Unit) : ApiViewingApp() {
    // Update record after [ApiViewingApp.retrieveConsuming] invocation
    override fun retrieveConsuming(target: Int, arg: Any?) {
        super.retrieveConsuming(target, arg)
        onRetrieved(this)
    }
}
