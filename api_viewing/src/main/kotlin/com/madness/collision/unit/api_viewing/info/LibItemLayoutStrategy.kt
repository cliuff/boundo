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

package com.madness.collision.unit.api_viewing.info

import android.util.Log

class LibItemLayoutStrategy(private val width: Float) {
    sealed interface Value
    class SimpleValue(val items: List<PackComponent>) : Value
    class GroupedValue(val entries: List<Pair<Int, List<PackComponent>>>) : Value

    fun calculate(itemList: List<PackComponent>, type: PackCompType, section: CompSection): Value? {
        when (type) {
            PackCompType.SharedLibrary,
            PackCompType.NativeLibrary, PackCompType.Activity, PackCompType.Service,
            PackCompType.Receiver, PackCompType.Provider -> return null
            PackCompType.DexPackage -> Unit
        }
        when (section) {
            CompSection.Marked -> return null
            CompSection.Normal, CompSection.MinimizedSelf, CompSection.Minimized -> Unit
        }
        val sizeGroups = itemList.groupBy {
            val valueLength = it.comp.value.length
            when (section) {
                CompSection.Marked -> Int.MAX_VALUE
                CompSection.Normal, CompSection.MinimizedSelf -> when (valueLength) {
                    in 0..24 -> 24
                    in 0..35 -> 35
                    else -> Int.MAX_VALUE
                }
                CompSection.Minimized -> when (valueLength) {
                    in 0..3 -> 3
                    in 0..6 -> 6
                    in 0..12 -> 12
                    in 0..24 -> 24
                    in 0..35 -> 35
                    else -> Int.MAX_VALUE
                }
            }
        }.toList()
        val (chunkGroups, normalGroups) = sizeGroups.partition { calculateRowSize(it.first) > 1 }
        val abortGroup = kotlin.run abort@{
            if (chunkGroups.isEmpty()) return@abort true
            if (chunkGroups.all { it.second.size <= 40 }) return@abort true
            false
        }
        if (abortGroup) {
            Log.d("LibItemLayoutStrategy", "calculate/width:$width/$section/group:none")
            return SimpleValue(itemList)
        }
        val (smallChunkGroups, otherChunkGroups) = chunkGroups.partition { it.second.size <= 40 }
        val mergedGroups = if (smallChunkGroups.isNotEmpty()) {
            // merge small lists and re-sort
            val maxSmallLimit = smallChunkGroups.maxOf { it.first }
            val p = smallChunkGroups.flatMap { it.second }.sortedBy { it.comp.value }
            val mergedChunk = listOf(maxSmallLimit to p)
            listOf(normalGroups, otherChunkGroups, mergedChunk).flatten()
        } else {
            sizeGroups
        }
        val sortedGroups = when (section) {
            CompSection.Marked, CompSection.Normal, CompSection.MinimizedSelf -> mergedGroups.sortedBy { it.first }
            CompSection.Minimized -> mergedGroups.sortedByDescending { it.first }
        }
        val msg = sortedGroups.joinToString(separator = ",") { (limit, list) ->
            val l = if (limit == Int.MAX_VALUE) "MAX" else limit.toString()
            "$l:${list.size}"
        }
        Log.d("LibItemLayoutStrategy", "calculate/width:$width/$section/group/$msg")
        return GroupedValue(sortedGroups)
    }

    fun calculateRowSize(sizeLimit: Int): Int {
        if (width <= 0) return -1
        return when (width) {
            // >0dp
            in 0f..200f -> when (sizeLimit) {
                3 -> 4
                6 -> 2
                12 -> -1
                24 -> -1
                else -> -1
            }
            // >200dp
            in 0f..320f -> when (sizeLimit) {
                3 -> 7
                6 -> 4
                12 -> 2
                24 -> -1
                else -> -1
            }
            // >320dp
            in 0f..540f -> when (sizeLimit) {
                3 -> 10
                6 -> 6
                12 -> 4
                24 -> 2
                else -> -1
            }
            // >540dp
            else -> when (sizeLimit) {
                3 -> 15
                6 -> 9
                12 -> 6
                24 -> 3
                35 -> 2
                else -> -1
            }
        }
    }
}
