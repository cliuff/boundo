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
import io.cliuff.boundo.org.data.model.toUpdate
import io.cliuff.boundo.org.db.dao.OrgAppDao
import io.cliuff.boundo.org.db.dao.OrgGroupDao
import io.cliuff.boundo.org.db.model.AppGroup
import io.cliuff.boundo.org.model.OrgGroup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface GroupRepository {
    suspend fun addGroupAndApps(collId: Int, group: OrgGroup): Int
    suspend fun updateGroupAndApps(group: OrgGroup)
    suspend fun updateGroup(group: OrgGroup)
    suspend fun removeGroup(collId: Int, group: OrgGroup): Boolean
    suspend fun getAppGroups(collId: Int): Map<String, List<OrgGroup>>
    fun getGroups(collId: Int): Flow<List<OrgGroup>>
}

class GroupRepoImpl(
    private val groupDao: OrgGroupDao,
    private val appDao: OrgAppDao,
) : GroupRepository {

    override suspend fun addGroupAndApps(collId: Int, group: OrgGroup): Int {
        val gid = groupDao.insert(group.toEntity(collId)).toInt()
        if (gid > 0) {
            val apps = group.apps.map { app -> app.toEntity(gid) }
            appDao.insertAll(apps)
        }
        return gid
    }

    override suspend fun updateGroupAndApps(group: OrgGroup) {
        groupDao.update(group.toUpdate())
        if (group.id > 0) {
            val apps = group.apps.map { app -> app.toEntity(group.id) }
            appDao.insertAll(apps)
        }
    }

    override suspend fun updateGroup(group: OrgGroup) {
        groupDao.update(group.toUpdate())
    }

    override suspend fun removeGroup(collId: Int, group: OrgGroup): Boolean {
        // delete related foreign table records automatically
        return groupDao.delete(group.toEntity(collId)) > 0
    }

    override suspend fun getAppGroups(collId: Int): Map<String, List<OrgGroup>> {
        return groupDao.selectOneOffColl(collId).map(AppGroup::toModel)
            .flatMap { group -> group.apps.map { app -> app.pkg to group } }
            .groupBy({ (pkg, _) -> pkg }) { (_, group) -> group }
    }

    override fun getGroups(collId: Int): Flow<List<OrgGroup>> {
        return groupDao.selectColl(collId).map { entities ->
            entities.map(AppGroup::toModel)
        }
    }
}
