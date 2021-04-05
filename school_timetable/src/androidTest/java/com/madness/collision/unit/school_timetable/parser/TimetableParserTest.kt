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

package com.madness.collision.unit.school_timetable.parser

import com.madness.collision.util.F
import org.junit.Test
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths

class TimetableParserTest {

    @Test
    fun parseRawTable() {
        val parentPath = "C:\\00a\\Documents\\SchoolTimetableHTML"
        val file = F.createFile(parentPath)
        file.list()?.forEach {
            val encoded = Files.readAllBytes(Paths.get(parentPath + "\\" + it))
            val re = String(encoded, Charset.defaultCharset())
            println(it)
            println(TimetableParser.parseRawTable(re))
        }
    }
}