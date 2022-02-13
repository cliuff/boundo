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

import android.content.Context
import androidx.room.*
import androidx.room.migration.AutoMigrationSpec
import com.madness.collision.unit.api_viewing.data.ApiViewingApp

@Database(
    version = 3,
    exportSchema = true,
    entities = [ApiViewingApp::class],
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3, spec = AppNameMigrationSpec::class),
    ]
)
@TypeConverters(Converters::class)
internal abstract class AppRoom : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppRoom? = null

        fun getDatabase(context: Context): AppRoom {
            return INSTANCE ?: synchronized(this) {
                val ctx = context.applicationContext
                INSTANCE ?: Room.databaseBuilder(ctx, AppRoom::class.java, "app").build()
                    .also { INSTANCE = it }
            }
        }

        fun clearInstance() {
            INSTANCE = null
        }
    }
}

@DeleteColumn(tableName = "app", columnName = "name")
class AppNameMigrationSpec : AutoMigrationSpec
