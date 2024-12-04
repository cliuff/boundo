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

import android.content.Context
import io.cliuff.boundo.org.db.OrgRoom

object OrgCollRepo {
    fun coll(context: Context): CollRepoImpl {
        val db = OrgRoom.getDatabase(context)
        return CollRepoImpl(db.collDao())
    }

    fun compColl(context: Context): CompCollRepoImpl {
        val db = OrgRoom.getDatabase(context)
        return CompCollRepoImpl(db.collDao())
    }
}