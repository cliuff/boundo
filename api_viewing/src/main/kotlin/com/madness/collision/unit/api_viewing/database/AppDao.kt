/*
 * Copyright 2021 Clifford Liu
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

package com.madness.collision.unit.api_viewing.database

import androidx.room.*
import com.madness.collision.unit.api_viewing.data.ApiUnit

@Dao
interface AppDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg app: AppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(apps: List<AppEntity>)

    @Delete
    fun delete(vararg app: AppEntity)

    @Delete
    fun delete(apps: List<AppEntity>)

    @Query("DELETE FROM app WHERE packageName = :packageName")
    fun deletePackageName(vararg packageName: String)

    /**
     * Delete records matching [packageNames]
     */
    @Query("DELETE FROM app WHERE packageName IN (:packageNames)")
    fun deletePackageNames(packageNames: List<String>)

    /**
     * Retain records matching [packageNames] and delete all others
     */
    @Query("DELETE FROM app WHERE packageName NOT IN (:packageNames)")
    fun deleteNonExistPackageNames(packageNames: List<String>)

    @Query("DELETE FROM app")
    fun deleteAll()

    fun selectAllApps(): List<AppEntity> = selectApps(ApiUnit.ALL_APPS)

    fun selectUserApps(): List<AppEntity> = selectApps(ApiUnit.USER)

    fun selectSystemApps(): List<AppEntity> = selectApps(ApiUnit.SYS)

    @Query("SELECT * FROM app WHERE (:unit = ${ApiUnit.ALL_APPS}) OR (apiUnit = :unit) ORDER BY packageName ASC")
    fun selectApps(unit: Int): List<AppEntity>

    @Query("SELECT * FROM app WHERE packageName = :packageName")
    fun selectApp(packageName: String): AppEntity?

    @Query("SELECT * FROM app WHERE packageName IN (:packageNames)")
    fun selectApps(packageNames: List<String>): List<AppEntity>

    @Query("SELECT COUNT(*) > 1 FROM app WHERE packageName = :packageName")
    fun selectIsExist(packageName: String): Boolean

    @Query("SELECT COUNT(*) FROM app")
    fun selectCount(): Int?

    @Query("SELECT updateTime FROM app WHERE packageName = :packageName")
    fun selectUpdateTime(packageName: String): Long?
}
