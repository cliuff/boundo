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
import io.cliuff.boundo.org.db.dao.OrgCollDao
import io.cliuff.boundo.org.model.CollInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface CollRepository {
    suspend fun addCollection(coll: CollInfo)
    suspend fun removeCollection(coll: CollInfo)
    fun getCollections(): Flow<List<CollInfo>>
}

class CollRepoImpl(private val collDao: OrgCollDao) : CollRepository {

    override suspend fun addCollection(coll: CollInfo) {
        collDao.insert(coll.toEntity())
    }

    override suspend fun removeCollection(coll: CollInfo) {
        collDao.delete(coll.toEntity())
    }

    override fun getCollections(): Flow<List<CollInfo>> {
        return collDao.selectAll().map { entities ->
            entities.map { ent ->
                ent.toModel(groupCount = 0)
            }
        }
    }
}
