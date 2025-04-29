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
import io.cliuff.boundo.org.db.dao.OrgCreateDao
import io.cliuff.boundo.org.db.dao.OrgGroupDao
import io.cliuff.boundo.org.db.model.AppGroup
import io.cliuff.boundo.org.model.OrgGroup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface GroupRepository {
    /** Add multiple groups and their apps, in transaction. */
    suspend fun addGroupsAndApps(collId: Int, groups: List<OrgGroup>): List<Int>
    /** Add a group and its apps, in transaction. */
    suspend fun addGroupAndApps(collId: Int, group: OrgGroup): Int
    suspend fun updateGroupAndApps(group: OrgGroup)
    suspend fun updateGroup(group: OrgGroup)
    suspend fun removeGroup(collId: Int, group: OrgGroup): Boolean
    /** Get group by [groupId]. */
    suspend fun getOneOffGroup(groupId: Int): OrgGroup?
    /** Get coll groups by [collId]. */
    suspend fun getOneOffGroups(collId: Int): List<OrgGroup>
    suspend fun getAppGroups(collId: Int): Map<String, List<OrgGroup>>
    /** Get group flow by [groupId]. */
    fun getGroup(groupId: Int): Flow<OrgGroup?>
    fun getGroups(collId: Int): Flow<List<OrgGroup>>
}

internal class GroupRepoImpl(
    private val createDao: OrgCreateDao,
    private val groupDao: OrgGroupDao,
    private val appDao: OrgAppDao,
) : GroupRepository {

    override suspend fun addGroupsAndApps(collId: Int, groups: List<OrgGroup>): List<Int> {
        val groupEntities = groups.map { it.toEntity(collId) }
        return createDao.insertGroups(groupEntities) { i, gid ->
            groups[i].apps.map { app -> app.toEntity(gid) }
        }
    }

    override suspend fun addGroupAndApps(collId: Int, group: OrgGroup): Int {
        val groupEnt = group.toEntity(collId)
        return createDao.insertGroupAndApps(groupEnt) { gid ->
            group.apps.map { app -> app.toEntity(gid) }
        }
    }

    override suspend fun updateGroupAndApps(group: OrgGroup) {
        val update = group.toUpdate()
        val apps = group.apps.map { app -> app.toEntity(group.id) }
        createDao.updateGroupAndApps(update, apps)
    }

    override suspend fun updateGroup(group: OrgGroup) {
        groupDao.update(group.toUpdate())
    }

    override suspend fun removeGroup(collId: Int, group: OrgGroup): Boolean {
        // delete related foreign table records automatically
        return groupDao.delete(group.toEntity(collId)) > 0
    }

    override suspend fun getOneOffGroup(groupId: Int): OrgGroup? {
        return groupDao.selectOneOff(groupId)?.let(AppGroup::toModel)
    }

    override suspend fun getOneOffGroups(collId: Int): List<OrgGroup> {
        return groupDao.selectOneOffColl(collId).map(AppGroup::toModel)
    }

    override suspend fun getAppGroups(collId: Int): Map<String, List<OrgGroup>> {
        return groupDao.selectOneOffColl(collId).map(AppGroup::toModel)
            .flatMap { group -> group.apps.map { app -> app.pkg to group } }
            .groupBy({ (pkg, _) -> pkg }) { (_, group) -> group }
    }

    override fun getGroup(groupId: Int): Flow<OrgGroup?> {
        return groupDao.select(groupId).map { ent -> ent?.toModel() }
    }

    override fun getGroups(collId: Int): Flow<List<OrgGroup>> {
        return groupDao.selectColl(collId).map { entities ->
            entities.map(AppGroup::toModel)
        }
    }
}
