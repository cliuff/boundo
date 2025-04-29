/*
 * Copyright 2025 Clifford Liu
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

package io.cliuff.boundo.org.db.dao

import androidx.room.Dao
import androidx.room.Transaction
import io.cliuff.boundo.org.db.OrgDatabase
import io.cliuff.boundo.org.db.model.OrgAppEntity
import io.cliuff.boundo.org.db.model.OrgGroupEntity
import io.cliuff.boundo.org.db.model.OrgGroupUpdate

@Dao
internal abstract class OrgCreateDao(db: OrgDatabase) {
    private val groupDao = db.groupDao()
    private val appDao = db.appDao()

    @Transaction
    open suspend fun insertGroups(groups: List<OrgGroupEntity>, apps: (Int, Int) -> List<OrgAppEntity>): List<Int> {
        return groups.mapIndexed { i, group ->
            insertGroupAndApps(group) { gid -> apps(i, gid) }
        }
    }

    @Transaction
    open suspend fun insertGroupAndApps(group: OrgGroupEntity, apps: (Int) -> List<OrgAppEntity>): Int {
        val gid = groupDao.insert(group).toInt()
        // replace with new apps instead of inserting all
        if (gid > 0) appDao.replace(gid, apps(gid))
        return gid
    }

    @Transaction
    open suspend fun updateGroupAndApps(update: OrgGroupUpdate, apps: List<OrgAppEntity>) {
        groupDao.update(update)
        if (update.id > 0) appDao.replace(update.id, apps)
    }
}