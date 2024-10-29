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

package io.cliuff.boundo.org.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import io.cliuff.boundo.org.db.model.OrgAppEntity
import io.cliuff.boundo.org.db.model.OrgCollEntity
import io.cliuff.boundo.org.db.model.OrgGroupEntity

@Database(
    version = 1,
    exportSchema = true,
    entities = [
        OrgCollEntity::class,
        OrgGroupEntity::class,
        OrgAppEntity::class,
    ],
)
internal abstract class OrgDatabase : RoomDatabase() {
}

internal object OrgRoom {
    @Volatile
    private var instance: OrgDatabase? = null

    fun getDatabase(context: Context): OrgDatabase {
        return instance ?: synchronized(this) {
            instance ?: OrgDatabaseImpl(context).also { instance = it }
        }
    }
}

private fun OrgDatabaseImpl(context: Context): OrgDatabase =
    Room.databaseBuilder(context.applicationContext, OrgDatabase::class.java, "org-db").build()
