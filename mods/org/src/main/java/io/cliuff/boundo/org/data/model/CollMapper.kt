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

package io.cliuff.boundo.org.data.model

import io.cliuff.boundo.org.db.model.AppColl
import io.cliuff.boundo.org.db.model.AppGroup
import io.cliuff.boundo.org.db.model.OrgCollEntity
import io.cliuff.boundo.org.model.CollInfo
import io.cliuff.boundo.org.model.CompColl

fun CollInfo.toEntity() =
    OrgCollEntity(
        id = id,
        name = name,
    )

fun OrgCollEntity.toModel(groupCount: Int) =
    CollInfo(
        id = id,
        name = name,
        groupCount = groupCount,
    )


fun CompColl.toEntity() =
    OrgCollEntity(
        id = id,
        name = name,
    )

fun AppColl.toModel() =
    CompColl(
        id = collEnt.id,
        name = collEnt.name,
        groups = groupEntities.map(AppGroup::toModel),
    )


fun List<CollInfo>.toEntities() = map(CollInfo::toEntity)
