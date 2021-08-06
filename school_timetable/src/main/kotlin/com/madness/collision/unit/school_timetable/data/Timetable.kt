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

package com.madness.collision.unit.school_timetable.data

import android.content.Context
import com.madness.collision.unit.school_timetable.calendar.ICal
import com.madness.collision.util.F
import com.madness.collision.util.X
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.Scanner
import kotlin.Comparator

/**
 * Timetable
 */
class Timetable(courses: List<CourseSingleton>) {

    companion object{

        fun getPersistenceFolder( context: Context): File{
            return File(F.valFilePubTtCache(context))
        }

        fun hasPersistence( context: Context): Boolean {
            val cacheDir = getPersistenceFolder(context)
            val files = cacheDir.listFiles() ?: emptyArray()
            return cacheDir.exists() && files.isNotEmpty()
        }

        fun clearPersistence( context: Context){
            if (hasPersistence(context)) X.deleteFolder(getPersistenceFolder(context))
        }

        fun  getPersistenceFile( context: Context): File?{
            val cacheDir = getPersistenceFolder(context)
            if (!cacheDir.exists()) return null
            val cacheFiles = cacheDir.listFiles() ?: emptyArray()
            if (cacheFiles.isEmpty()) return null
            var cacheFile = cacheFiles[0]
            for (file in cacheFiles){
                if (file.lastModified() > cacheFile.lastModified())
                    cacheFile = file
            }
            return cacheFile
        }

        fun  fromPersistence( context: Context,  path: String = ""): Timetable {
            val cacheFile = if (path.isEmpty()) getPersistenceFile(context) else File(path)
            if (cacheFile == null || !cacheFile.exists()) return Timetable()
            return fromPersistence(cacheFile)
        }

        fun fromPersistence( cacheFile: File): Timetable {
            val input: FileInputStream
            try {
                input = FileInputStream(cacheFile)
            } catch ( e: FileNotFoundException) {
                e.printStackTrace()
                return Timetable()
            }
            val cacheIn = Scanner(input)
            var inString: String
            var items: List<String>
            val courseList = mutableListOf<CourseSingleton>()
            while (cacheIn.hasNext()){
                inString = cacheIn.nextLine()
                if (inString == null || inString.isEmpty())
                    continue
                items = inString.split(";")
                val cs = CourseSingleton()
                for (item in items){
                    val fillIn = item.substring(item.indexOf(':') + 1)
                    when (item.substring(0, item.indexOf(':'))){
                        "cN" -> cs.name = fillIn
                        "cT" -> cs.educating.educator = fillIn
                        "cR" -> cs.educating.location = fillIn
                        "cP" -> {
                            var repetitions = Repetition.parseStandard(fillIn)
                            if (repetitions.isEmpty()) repetitions = Repetition.parseRaw(fillIn)
                            cs.educating.repetitions = repetitions
                        }
                        "cW" -> cs.educating.dayOfWeek = fillIn.toInt()
                        "cTl" -> cs.legacyTemplate = fillIn[0]
                        "clP" -> cs.legacyClassPeriod = fillIn[0]
                        "clPh" ->
                            when (fillIn){
                                "amI" -> "11"
                                "amII" -> "12"
                                "pmI" -> "21"
                                "pmII" -> "22"
                                "pmIII" -> "23"
                                "eve" -> "31"
                                "eveII" -> "32"
                                else -> fillIn
                            }.let { cs.legacyClassPhase = it }
                        "dpt" -> cs.legacyDuplicate = fillIn.toInt() == 1
//                    "null" ->
                    }
                }
                if (cs.name.isNotEmpty()) courseList.add(cs)
            }
            cacheIn.close()
            val timetable = Timetable(courseList)
            timetable.renderTimetable()
            timetable.renderUID()
            return timetable
        }

        fun  produceWeekIndicator( context: Context, endWeek: Int): Boolean{
            ICal.loadTime(context)
            val print = ICal.getWeekIndicatorComplete(context, endWeek)
            val path = F.valFilePubTtIndicator(context)
            return write(print, path)
        }

        private fun write(content: String, path: String): Boolean{
            val ushContent = content.toByteArray(StandardCharsets.UTF_8)
            val file = File(path)
            if (!F.prepare4(file)) return false
            try {
                FileOutputStream(file).use { it.write(ushContent) }
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return true
        }
    }

    val courses: MutableList<CourseSingleton> = if (courses is MutableList) courses else courses.toMutableList()
    var rows = 0
    var columns = 0
    var rowAm = 0
    var rowPm = 0
    var rowEve = 0

    constructor(): this(emptyList())

    fun renderTimetable(){
        var am1 = 0
        var am2 = 0
        var am3 = 0
        var pm1 = 0
        var pm2 = 0
        var pm3 = 0
        var eve1 = 0
        var eve2 = 0
        var eve3 = 0
        var weekend = 0
        for (course in courses){
            when (course.legacyClassPhase){
                "11" -> am1++
                "12" -> am2++
                "13" -> am3 ++
                "21" -> pm1++
                "22" -> pm2++
                "23" -> pm3++
                "31" -> eve1++
                "32" -> eve2++
                "33" -> eve3 ++
            }
            val dayOfWeek = course.educating.dayOfWeek
            if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY_LEGACY)
                weekend++
        }
        rowAm = getRows(am1, am2, am3)
        rowPm = getRows(pm1, pm2, pm3)
        rowEve = getRows(eve1, eve2, eve3)
        rows = rowAm + rowPm + rowEve
        columns = if (weekend == 0) 5 else 7
        for (course in courses){
            val phase = course.legacyClassPhase
            val dayOfWeek = course.educating.dayOfWeek
            var num = phase[1].toInt() - 49
            when (phase[0]){
//                '1' ->
                '2' -> num += rowAm
                '3' -> num += (rowAm + rowPm)
            }
            course.legacyClassSchedule = num * columns + dayOfWeek
        }

        courses.sortWith(Comparator { o1, o2 ->
            o1.legacyClassSchedule.compareTo(o2.legacyClassSchedule)
        })
    }

    fun renderUID(){
        for (course in courses) course.renderUid()
    }

    private fun getRows(period1: Int, period2: Int, period3: Int): Int{
        var p1 = period1
        var p2 = period2
        var p3 = period3
        if (period1 > 0) p1 = 1
        if (period2 > 0) p2 = 2
        if (period3 > 0) p3 = 4
        val binary = p1 + p2 + p3
        return when {
            binary >= 4 -> 3
            binary >= 2 -> 2
            else -> binary
        }
    }

    fun produceICal( context: Context): Boolean{
        ICal.loadTime(context)
        val print = ICal.get(courses)
        val path = F.valFilePubTtCurrent(context)
        return write(print, path)
    }

    fun persist( context: Context, clearPersistence: Boolean = true){
        if (clearPersistence) clearPersistence(context)
        persist(context)
    }

    /**
     * persist data to external storage
     */
    fun persist( context: Context){
        val newPathFile = getPersistenceFolder(context)
        if (!F.prepareDir(newPathFile)) return
        val cacheFile: File
        try {
            cacheFile = File.createTempFile("cache", ".txt", newPathFile)
        } catch ( e: IOException) {
            e.printStackTrace()
            return
        }
        val cacheStream: FileOutputStream
        try {
            cacheStream = FileOutputStream(cacheFile)
        } catch ( e: FileNotFoundException) {
            e.printStackTrace()
            return
        }
        var out: StringBuilder
        for (course in courses){
//            if (course == null) continue
            out = StringBuilder()
            out.append("cN:").append(course.name)
            out.append(";cP:").append(Repetition.toStandardString(course.educating.repetitions))
            out.append(";cW:").append(course.educating.dayOfWeek)
            out.append(";cTl:").append(course.legacyTemplate)
            out.append(";clP:").append(course.legacyClassPeriod)
            out.append(";clPh:").append(course.legacyClassPhase)
            out.append(";cT:").append(course.educating.educator)
            out.append(";cR:").append(course.educating.location)
            out.append(";dpt:")
            out.append(if (course.legacyDuplicate) 1 else 0)
            out.append("\n")
            try {
                cacheStream.write(out.toString().toByteArray(StandardCharsets.UTF_8))
            } catch ( e: IOException) {
                e.printStackTrace()
            }
        }
        try {
            cacheStream.close()
        } catch ( e: IOException) {
            e.printStackTrace()
        }
    }
}
