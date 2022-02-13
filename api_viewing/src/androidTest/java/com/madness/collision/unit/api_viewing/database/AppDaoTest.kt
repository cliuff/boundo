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

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class AppDaoTest {
    private lateinit var room: AppRoom
    private lateinit var dao: AppDao

    @Before
    fun setUp() {
        room = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                AppRoom::class.java
        ).allowMainThreadQueries().build()
        dao = room.appDao()
    }

    @After
    fun tearDown() {
        room.close()
    }
}