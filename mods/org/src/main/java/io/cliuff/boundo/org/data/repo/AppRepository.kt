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

package io.cliuff.boundo.org.data.repo

import io.cliuff.boundo.org.data.model.toEntity
import io.cliuff.boundo.org.data.model.toModel
import io.cliuff.boundo.org.db.dao.OrgAppDao
import io.cliuff.boundo.org.db.model.OrgAppEntity
import io.cliuff.boundo.org.model.OrgApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface AppRepository {
    suspend fun addApp(groupId: Int, app: OrgApp)
    suspend fun removeApp(groupId: Int, app: OrgApp)
    fun getApps(groupId: Int): Flow<List<OrgApp>>
}

class AppRepoImpl(private val appDao: OrgAppDao) : AppRepository {
    override suspend fun addApp(groupId: Int, app: OrgApp) {
        appDao.insert(app.toEntity(groupId))
    }

    override suspend fun removeApp(groupId: Int, app: OrgApp) {
        appDao.delete(app.toEntity(groupId))
    }

    override fun getApps(groupId: Int): Flow<List<OrgApp>> {
        return appDao.selectGroup(groupId).map { entities ->
            entities.map(OrgAppEntity::toModel)
        }
    }
}
