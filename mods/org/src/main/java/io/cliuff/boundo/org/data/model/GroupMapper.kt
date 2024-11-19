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

import io.cliuff.boundo.org.db.model.OrgGroupEntity
import io.cliuff.boundo.org.db.model.OrgGroupUpdate
import io.cliuff.boundo.org.model.OrgApp
import io.cliuff.boundo.org.model.OrgGroup

fun OrgGroup.toUpdate() =
    OrgGroupUpdate(
        id = id,
        name = name,
    )

fun OrgGroup.toEntity(collId: Int) =
    OrgGroupEntity(
        id = id,
        collId = collId,
        name = name,
    )

fun OrgGroupEntity.toModel(apps: List<OrgApp>) =
    OrgGroup(
        id = id,
        name = name,
        apps = apps,
    )
