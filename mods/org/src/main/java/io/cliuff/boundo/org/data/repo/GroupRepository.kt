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
import io.cliuff.boundo.org.db.dao.OrgGroupDao
import io.cliuff.boundo.org.db.model.OrgAppEntity
import io.cliuff.boundo.org.model.OrgGroup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface GroupRepository {
    suspend fun addGroup(collId: Int, group: OrgGroup)
    suspend fun updateGroup(group: OrgGroup)
    suspend fun removeGroup(collId: Int, group: OrgGroup)
    fun getGroups(collId: Int): Flow<List<OrgGroup>>
}

class GroupRepoImpl(private val groupDao: OrgGroupDao) : GroupRepository {
    override suspend fun addGroup(collId: Int, group: OrgGroup) {
        groupDao.insert(group.toEntity(collId))
    }

    override suspend fun updateGroup(group: OrgGroup) {
        groupDao.update(group.toUpdate())
    }

    override suspend fun removeGroup(collId: Int, group: OrgGroup) {
        groupDao.delete(group.toEntity(collId))
    }

    override fun getGroups(collId: Int): Flow<List<OrgGroup>> {
        return groupDao.selectColl(collId).map { entities ->
            entities.map { ent ->
                val pkgSet = ent.appEntities
                    .run { mapTo(HashSet(size), OrgAppEntity::pkgName) }
                ent.groupEnt.toModel(pkgSet)
            }
        }
    }
}
