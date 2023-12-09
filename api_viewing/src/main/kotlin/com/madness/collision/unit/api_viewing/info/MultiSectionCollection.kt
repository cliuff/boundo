/*
 * Copyright 2022 Clifford Liu
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

import java.util.Collections

interface MultiSectionCollection<Section, out Item> {
    val size: Int
    val sectionSize: Int
    val sectionItems: List<List<Item>>
    operator fun get(section: Section): List<Item>
    operator fun get(section: Section, index: Int): Item = this[section][index]
    fun getOrNull(section: Section, index: Int): Item? = this[section].getOrNull(index)
    fun entryIterator(): Iterator<Map.Entry<Section, List<Item>>>
    fun sectionIterator(): Iterator<List<Item>>
}

open class SectionMapCollection<Section, Item> : MultiSectionCollection<Section, Item> {
    // use linked map to preserve insertion order
    private val sectionMap: MutableMap<Section, List<Item>> = Collections.synchronizedMap(LinkedHashMap())
    // use copy to avoid ConcurrentModificationException during iteration
    private val sectionsCopy: Map<Section, List<Item>> get() = sectionMap.toMap()
    override val size: Int get() = sectionsCopy.values.sumOf { it.size }
    override val sectionSize: Int get() = sectionMap.size
    override val sectionItems: List<List<Item>> get() = sectionMap.values.toList()

    override fun get(section: Section): List<Item> {
        return sectionMap[section].orEmpty()
    }

    operator fun set(section: Section, items: List<Item>) {
        sectionMap[section] = items
    }

    override fun entryIterator(): Iterator<Map.Entry<Section, List<Item>>> = sectionsCopy.entries.iterator()
    override fun sectionIterator(): Iterator<List<Item>> = sectionsCopy.values.iterator()
}
