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
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.madness.collision.unit.api_viewing.data.ApiViewingApp

@Database(entities = [ApiViewingApp::class], version = 1)
@TypeConverters(Converters::class)
internal abstract class AppRoom : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppRoom? = null

        fun getDatabase(context: Context): AppRoom {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, AppRoom::class.java, "app")
                        .build().also { INSTANCE = it }
            }
        }
    }
}
