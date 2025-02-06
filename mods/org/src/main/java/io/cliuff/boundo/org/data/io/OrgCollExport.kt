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

package io.cliuff.boundo.org.data.io

import android.content.Context
import androidx.collection.MutableIntIntMap
import androidx.collection.mutableIntObjectMapOf
import io.cliuff.boundo.org.db.OrgRoom
import io.cliuff.boundo.org.db.model.OrgAppEntity
import io.cliuff.boundo.org.db.model.OrgCollEntity
import io.cliuff.boundo.org.db.model.OrgGroupEntity
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull
import java.io.InputStream
import java.io.OutputStream

object OrgCollExport {

    suspend fun toStream(output: OutputStream, collId: Int, context: Context) {
        val db = OrgRoom.getDatabase(context)
        val coll = withTimeoutOrNull(200) {
            db.collDao().select(collId).firstOrNull()
        }
        coll ?: return
        output.bufferedWriter().use { writer ->
            val gidMap = MutableIntIntMap(coll.groupEntities.size)
            var newGidInc = 0
            for (group in coll.groupEntities) {
                gidMap[group.groupEnt.id] = ++newGidInc
                writer.write(group.groupEnt.run { "+$newGidInc\t$name" })
                writer.newLine()
            }
            for (group in coll.groupEntities) {
                val gid = gidMap[group.groupEnt.id]
                for (app in group.appEntities) {
                    writer.write("#$gid\t${app.pkgName}\t${app.label}")
                    writer.newLine()
                }
            }
        }
    }

    suspend fun fromStream(input: InputStream, context: Context) {
        val groups = mutableIntObjectMapOf<String>()
        val apps = mutableListOf<Triple<Int, String, String>>()
        input.bufferedReader().useLines { lineSeq ->
            val groupRegex = """^\+(\d+)\s+(.+)$""".toRegex()
            val appRegex = """^#(\d+)\s+([\w.]+)\s+(.+)$""".toRegex()
            for (line in lineSeq) {
                val matchApp = appRegex.matchEntire(line)
                if (matchApp != null) {
                    val (gid, pkgName, label) = matchApp.destructured
                    apps += Triple(gid.toInt(), pkgName, label)
                    continue
                }
                val matchGroup = groupRegex.matchEntire(line)
                if (matchGroup != null) {
                    val (gid, label) = matchGroup.destructured
                    groups[gid.toInt()] = label
                    continue
                }
            }
        }

        val db = OrgRoom.getDatabase(context)
        val collDao = db.collDao()
        val groupDao = db.groupDao()
        val appDao = db.appDao()
        val time = System.currentTimeMillis()
        val coll = OrgCollEntity(0, "Import Coll", time, time)
        val cid = collDao.insert(coll).toInt()
        if (cid > 0) {
            val gidMap = MutableIntIntMap(groups.size)
            groups.forEach { k, v ->
                val ent = OrgGroupEntity(0, cid, v, time, time)
                gidMap[k] = groupDao.insert(ent).toInt()
            }
            apps.forEach { (gid, pkgName, label) ->
                val ent = OrgAppEntity(gidMap[gid], pkgName, label, "", time, time)
                appDao.insert(ent)
            }
        }
    }
}
