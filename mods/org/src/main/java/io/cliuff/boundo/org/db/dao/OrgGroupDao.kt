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

package io.cliuff.boundo.org.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.cliuff.boundo.org.db.model.AppGroup
import io.cliuff.boundo.org.db.model.OrgGroupEntity
import io.cliuff.boundo.org.db.model.OrgGroupUpdate
import kotlinx.coroutines.flow.Flow

@Dao
interface OrgGroupDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(group: OrgGroupEntity): Long

    @Update(entity = OrgGroupEntity::class)
    suspend fun update(group: OrgGroupUpdate)

    @Delete
    suspend fun delete(group: OrgGroupEntity): Int

    @Transaction
    @Query("SELECT * FROM org_group WHERE coll_id=:collId")
    suspend fun selectOneOffColl(collId: Int): List<AppGroup>

    @Transaction
    @Query("SELECT * FROM org_group WHERE coll_id=:collId")
    fun selectColl(collId: Int): Flow<List<AppGroup>>
}