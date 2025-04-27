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
import io.cliuff.boundo.org.model.OrgGroup
import java.util.TreeSet

fun CollInfo.toEntity() =
    OrgCollEntity(
        id = id,
        name = name,
        createTime = createTime,
        modifyTime = modifyTime,
    )

fun OrgCollEntity.toModel(groupCount: Int) =
    CollInfo(
        id = id,
        name = name,
        createTime = createTime,
        modifyTime = modifyTime,
        groupCount = groupCount,
    )


fun CompColl.toEntity() =
    OrgCollEntity(
        id = id,
        name = name,
        createTime = createTime,
        modifyTime = modifyTime,
    )

fun AppColl.toModel(compGroup: Comparator<OrgGroup>) =
    CompColl(
        id = collEnt.id,
        name = collEnt.name,
        createTime = collEnt.createTime,
        modifyTime = collEnt.modifyTime,
        groups = groupEntities.toModels(compGroup),
    )


fun List<CollInfo>.toEntities() = map(CollInfo::toEntity)


private fun List<AppGroup>.toModels(comparator: Comparator<OrgGroup>): List<OrgGroup> {
    // fall back to comparing id to guarantee inequality for TreeSet
    val treeSet = TreeSet(comparator.thenBy(OrgGroup::id))
    for (ent in this) treeSet.add(ent.toModel())
    return treeSet.toList()
}
