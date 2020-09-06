/*
 * Copyright 2020 Clifford Liu
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

package com.madness.collision.util

import com.google.common.truth.Truth
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.util.*

internal class StringUtilsTest {

    @Test
    fun compareName() {
        val locales = listOf(Locale("en"), Locale("zh"))
        val names = listOf("重庆市民通", "墨墨背单词", "bilibili", "酷安", "Google")
        val expected = listOf(names[2], names[4], names[3], names[1], names[0])
        val sorted = names.sortedWith { s1, s2 ->
            StringUtils.compareName(locales, s1, s2)
        }
        Truth.assertThat(sorted).isEqualTo(expected)
    }
}