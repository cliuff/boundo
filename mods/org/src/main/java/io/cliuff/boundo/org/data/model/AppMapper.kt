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

import io.cliuff.boundo.org.db.model.OrgAppEntity
import io.cliuff.boundo.org.db.model.OrgAppUpdate
import io.cliuff.boundo.org.model.OrgApp

fun OrgApp.toEntity(groupId: Int) =
    OrgAppEntity(
        groupId = groupId,
        pkgName = pkg,
        label = label,
        labelLocale = labelLocale,
        createTime = createTime,
        modifyTime = modifyTime,
    )

fun OrgAppEntity.toUpdate() =
    OrgAppUpdate(
        groupId = groupId,
        pkgName = pkgName,
        label = label,
        labelLocale = labelLocale,
        modifyTime = modifyTime,
    )

fun OrgAppEntity.toModel() =
    OrgApp(
        pkg = pkgName,
        label = label,
        labelLocale = labelLocale,
        createTime = createTime,
        modifyTime = modifyTime,
    )
