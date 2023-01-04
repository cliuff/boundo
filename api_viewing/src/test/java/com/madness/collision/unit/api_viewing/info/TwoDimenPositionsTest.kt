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

import org.junit.Test

internal class TwoDimenPositionsTest {

    @Test
    fun getCurrentIndex() {
        val map = mapOf(0 to 1, 1 to 5, 2 to 12, 3 to 23)
        var lastIndex = 0
        val m1 = (0..50).map { i ->
            TwoDimenPositions.getCurrentIndex(i, map, lastIndex).also { lastIndex = it }
        }
        val m2 = (0..50).reversed().map { i ->
            TwoDimenPositions.getCurrentIndex(i, map, lastIndex).also { lastIndex = it }
        }
        assert(m1 == m2.reversed()) {
            val s1 = m1.joinToString(prefix = "[", postfix = "]") { it.toString() }
            val s2 = m2.joinToString(prefix = "[", postfix = "]") { it.toString() }
            "$s1\n$s2"
        }
    }
}