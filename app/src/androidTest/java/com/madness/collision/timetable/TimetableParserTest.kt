package com.madness.collision.unit.school_timetable

import com.madness.collision.unit.school_timetable.parser.TimetableParser
import com.madness.collision.util.F
import org.junit.jupiter.api.Test
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