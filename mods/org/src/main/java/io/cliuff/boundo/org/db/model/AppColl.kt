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

package io.cliuff.boundo.org.db.model

import androidx.room.Embedded
import androidx.room.Relation

data class AppColl(
    @Embedded
    val collEnt: OrgCollEntity,
    @Relation(parentColumn = "_id", entityColumn = "coll_id", entity = OrgGroupEntity::class)
    val groupEntities: List<AppGroup>,
)

data class AppGroup(
    @Embedded
    val groupEnt: OrgGroupEntity,
    @Relation(parentColumn = "_id", entityColumn = "group_id")
    val appEntities: List<OrgAppEntity>,
)