/*
 * Copyright 2021 Clifford Liu
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

import com.google.common.truth.Truth.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Test

internal class PrefsUtilTest {

    @Test
    internal fun testFromJson() {
        val mockJson = """{"Key1":"1","Key2":"2"}"""
        val type = TypeToken.getParameterized(Map::class.java, String::class.java, String::class.java).type
        val reMap: Map<String, String> = Gson().fromJson(mockJson, type) ?: emptyMap()
        assertThat(reMap["Key1"]).isEqualTo("1")
        assertThat(reMap["Key2"]).isEqualTo("2")
    }

    @Test
    internal fun testToJson() {
        val mockMap = mapOf("Key1" to "1", "Key2" to "2")
        val type = TypeToken.getParameterized(Map::class.java, String::class.java, String::class.java).type
        val reString = Gson().toJson(mockMap, type)
        assertThat(reString).isEqualTo("""{"Key1":"1","Key2":"2"}""")
    }
}