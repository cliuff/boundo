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
import io.cliuff.boundo.org.db.model.OrgAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrgAppDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: OrgAppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<OrgAppEntity>)

    @Delete
    suspend fun delete(app: OrgAppEntity)

    @Query("DELETE FROM org_app WHERE pkg IN (:pkgs)")
    suspend fun deletePkgs(pkgs: List<String>): Int

    @Transaction
    suspend fun replace(groupId: Int, entities: List<OrgAppEntity>) {
        val newPkgs = entities.mapTo(HashSet(), OrgAppEntity::pkgName)
        val existingPkgs = selectGroupPkgs(groupId)
        if (existingPkgs.isNotEmpty()) {
            deletePkgs(existingPkgs - newPkgs)
        }
        insertAll(entities)
    }

    @Query("SELECT pkg FROM org_app WHERE group_id=:groupId")
    suspend fun selectGroupPkgs(groupId: Int): List<String>

    @Query("SELECT * FROM org_app WHERE group_id=:groupId")
    fun selectGroup(groupId: Int): Flow<List<OrgAppEntity>>
}