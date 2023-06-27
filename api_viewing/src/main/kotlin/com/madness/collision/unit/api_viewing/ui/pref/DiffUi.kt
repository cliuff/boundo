/*
 * Copyright 2023 Clifford Liu
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

package com.madness.collision.unit.api_viewing.ui.pref

import android.content.Context
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import com.madness.collision.unit.api_viewing.database.AppRoom
import com.madness.collision.unit.api_viewing.database.maintainer.DiffChange
import com.madness.collision.util.collection.groupConsecBy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

typealias DiffRecord = List<DiffChange>

sealed interface DiffUi {
    class Empty(val record: DiffRecord) : DiffUi
    class Change(val record: DiffRecord) : DiffUi
    class ExpandAction(val list: List<DiffRecord>) : DiffUi
}

object DiffUiData {
    suspend fun retrieve(context: Context): SnapshotStateList<DiffUi> {
        return withContext(Dispatchers.IO) { retList(context).toMutableStateList() }
    }

    fun expand(list: MutableList<DiffUi>, index: Int) {
        expandList(list, index)
    }
}

private suspend fun retList(context: Context): List<DiffUi> {
    val changes = AppRoom.getDatabase(context).diffDao().selectAll()
    val comparator = compareByDescending<DiffChange> { it.diff.timeMills }
        .thenBy { it.diff.packageName }
    val recordMap: Map<String, DiffRecord> = changes.sortedWith(comparator).groupBy { it.diff.id }
    // group consecutive empty records together
    val consecGroups = recordMap.values.groupConsecBy { a, b -> a[0].isNone == b[0].isNone }
    val resultList = arrayListOf<DiffUi>()
    for (i in consecGroups.indices) {
        val list = consecGroups[i]
        val isEmpty = list[0][0].isNone
        // minimize at least 2 items. size-minimize: 2-0, 3-0, 4-2, 5-3, ...
        val isMinimize = list.size > 3 && isEmpty
        val pendingPair = when {
            // use subList() for the second list, only 2 items are extra
            isMinimize -> listOf(list.subList(0, 2), list.subList(2, list.size))
            else -> listOf(list)
        }
        pendingPair.forEachIndexed { pI, pList ->
            when {
                isMinimize && pI > 0 -> resultList.add(DiffUi.ExpandAction(pList))
                isEmpty -> resultList.addAll(pList.map { DiffUi.Empty(it) })
                else -> resultList.addAll(pList.map { DiffUi.Change(it) })
            }
        }
    }
    return resultList
}

private fun expandList(list: MutableList<DiffUi>, index: Int) {
    val item = list[index]
    if (item !is DiffUi.ExpandAction) return
    val additions = item.list.map { DiffUi.Empty(it) }
    list.removeAt(index)
    list.addAll(index, additions)
}