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
import io.cliuff.boundo.org.db.model.AppColl
import io.cliuff.boundo.org.model.CompColl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface CompCollRepository {
    fun getCompCollection(collId: Int): Flow<CompColl?>
    fun getCompCollections(): Flow<List<CompColl>>
    suspend fun removeCompCollection(coll: CompColl): Boolean
}

class CompCollRepoImpl(private val collDao: OrgCollDao) : CompCollRepository {

    override fun getCompCollection(collId: Int): Flow<CompColl?> {
        return collDao.select(collId).map { compEnt ->
            compEnt?.toModel()
        }
    }

    override fun getCompCollections(): Flow<List<CompColl>> {
        return collDao.selectAllComp().map { compEnts ->
            compEnts.map(AppColl::toModel)
        }
    }

    override suspend fun removeCompCollection(coll: CompColl): Boolean {
        // delete related foreign table records automatically
        return collDao.delete(coll.toEntity()) > 0
    }
}
