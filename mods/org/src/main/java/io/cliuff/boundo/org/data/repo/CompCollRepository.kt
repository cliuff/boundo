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

import io.cliuff.boundo.org.data.model.toModel
import io.cliuff.boundo.org.db.dao.OrgCollDao
import io.cliuff.boundo.org.db.model.OrgAppEntity
import io.cliuff.boundo.org.model.CompColl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface CompCollRepository {
    fun getCompCollection(): Flow<CompColl?>
}

class CompCollRepoImpl(private val collDao: OrgCollDao) : CompCollRepository {

    override fun getCompCollection(): Flow<CompColl?> {
        return collDao.select(1).map coll@{ coll ->
            coll ?: return@coll null
            val groups = coll.groupEntities.map { group ->
                val pkgSet = group.appEntities
                    .run { mapTo(HashSet(size), OrgAppEntity::pkgName) }
                group.groupEnt.toModel(pkgSet)
            }
            CompColl(id = coll.collEnt.id, name = coll.collEnt.name, groups = groups)
        }
    }
}
