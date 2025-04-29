/*
 * Copyright 2025 Clifford Liu
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

package io.cliuff.boundo.org.data.usecase

import io.cliuff.boundo.org.data.repo.CollRepository
import io.cliuff.boundo.org.data.repo.GroupRepository
import io.cliuff.boundo.org.model.CollInfo
import io.cliuff.boundo.org.model.OrgApp
import io.cliuff.boundo.org.model.OrgGroup

class CollUseCase(
    private val collRepo: CollRepository,
    private val groupRepo: GroupRepository,
    private val provideLabel: (pkgName: String) -> String?,
) {
    suspend fun createColl(collName: String, groups: List<Pair<String, Set<String>>>): Int {
        val time = System.currentTimeMillis()
        // Create a new coll with non-blank coll name, or default to unnamed.
        val updCollName = collName.trim().takeUnless { it.isBlank() } ?: "Unnamed Coll"
        val createColl = CollInfo(0, updCollName, time, time, 0)
        val collId = collRepo.addCollection(createColl)
        if (collId <= 0) return collId

        val groupList = groups.map { (groupName, pkgs) ->
            // Take non-blank group name, or default to unnamed.
            val updGroupName = groupName.trim().takeUnless { it.isBlank() } ?: "Unnamed Group"
            val apps = pkgs.map { pkg ->
                OrgApp(pkg, provideLabel(pkg) ?: "", "", time, time)
            }
            OrgGroup(0, updGroupName, time, time, apps)
        }
        // Create all groups in transaction.
        groupRepo.addGroupsAndApps(collId, groupList)
        return collId
    }

    suspend fun createGroup(modCid: Int, collName: String, groupName: String, pkgs: Set<String>): Pair<Int, Int> {
        val time = System.currentTimeMillis()
        // Create a new coll with non-blank coll name, or default to unnamed.
        val updCollName = collName.trim().takeUnless { it.isBlank() } ?: "Unnamed Coll"
        val collId = if (modCid <= 0) {
            val createColl = CollInfo(0, updCollName, time, time, 0)
            collRepo.addCollection(createColl)
        } else {
            modCid
        }

        // Take non-blank group name, or default to unnamed.
        val updGroupName = groupName.trim().takeUnless { it.isBlank() } ?: "Unnamed Group"
        val apps = pkgs.map { pkg ->
            OrgApp(pkg, provideLabel(pkg) ?: "", "", time, time)
        }
        if (collId > 0) {
            // Create a new group with supplied data.
            val updGroup = OrgGroup(0, updGroupName, time, time, apps)
            val gid = groupRepo.addGroupAndApps(collId, updGroup)
            return if (gid > 0) collId to gid else collId to -1
        }
        return -1 to -1
    }

    suspend fun modifyGroup(modGid: Int, groupName: String, pkgs: Set<String>): Int {
        val time = System.currentTimeMillis()
        // Take non-blank group name, or default to unnamed.
        val updGroupName = groupName.trim().takeUnless { it.isBlank() } ?: "Unnamed Group"
        val apps = pkgs.map { pkg ->
            OrgApp(pkg, provideLabel(pkg) ?: "", "", time, time)
        }
        // Update group with new data.
        val updGroup = OrgGroup(modGid, updGroupName, time, time, apps)
        groupRepo.updateGroupAndApps(updGroup)
        return modGid
    }
}