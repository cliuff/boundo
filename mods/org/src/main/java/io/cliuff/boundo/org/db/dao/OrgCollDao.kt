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
import io.cliuff.boundo.org.db.model.AppColl
import io.cliuff.boundo.org.db.model.OrgCollEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrgCollDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(coll: OrgCollEntity)

    @Delete
    suspend fun delete(coll: OrgCollEntity)

    @Query("SELECT * FROM org_coll")
    fun selectAll(): Flow<List<OrgCollEntity>>

    @Transaction
    @Query("SELECT * FROM org_coll WHERE _id=:collId")
    fun select(collId: Int): Flow<AppColl?>
}