package com.madness.collision.unit.school_timetable.calendar

import android.content.Context
import com.madness.collision.R
import com.madness.collision.unit.school_timetable.data.CourseSingleton
import com.madness.collision.unit.school_timetable.data.TimetablePeriod
import com.madness.collision.util.P
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

internal object ICal {
    const val HEADER_SRC =
            """
                    BEGIN:VCALENDAR
                    VERSION:2.0
                    CALSCALE:GREGORIAN
                    X-WR-TIMEZONE:Asia/Shanghai
                    BEGIN:VTIMEZONE
                    TZID:Asia/Shanghai
                    X-LIC-LOCATION:Asia/Shanghai
                    BEGIN:STANDARD
                    TZOFFSETFROM:+0800
                    TZOFFSETTO:+0800
                    TZNAME:CST
                    DTSTART:19700101T000000
                    END:STANDARD
                    END:VTIMEZONE

                    """
    val HEADER get() = HEADER_SRC.trimIndent()
    const val FOOTER = "\r\nEND:VCALENDAR\r\n"
    const val HEADER_EVENT = "\r\nBEGIN:VEVENT"
    const val FOOTER_EVENT = "\r\nEND:VEVENT"
    private const val TIME_MORNING = 1
    private const val TIME_AFTERNOON = 2
    private const val TIME_EVENING = 3

    private val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.CHINA)
    private val timeFormatSeconds = SimpleDateFormat("kkmmss", Locale.CHINA)
    private val timeFormat = SimpleDateFormat("kkmm", Locale.CHINA)

    //时间格式：8:30->830, 15:55->1555, 1 hour 30 minutes->90, 150 minutes->150
    //第一周周一（周日为周首日）、大课持续时间、早晨第一节课开始时间、下午开始时间、晚上开始时间、两大课之间休息时间
    private var original_date_start: String = ""
    private var morning_time: String = ""
    private var afternoon_time: String = ""
    private var evening_time: String = ""
    private var singleClassTime: Int = 0
    private var morningBreakTimeInferior: Int = 0
    private var morningBreakTimeSuperior: Int = 0
    private var afternoonBreakTimeInferior: Int = 0
    private var afternoonBreakTimeSuperior: Int = 0
    private var eveningBreakTimeInferior: Int = 0
    private var eveningBreakTimeSuperior: Int = 0
    private var appModeEnabled: Boolean = true

    fun loadTime(context: Context){
        val prefTT = context.getSharedPreferences(P.PREF_TIMETABLE, Context.MODE_PRIVATE)
        original_date_start = prefTT.getString(P.TT_DATE_START, P.TT_DATE_START_DEFAULT) ?: ""
        morning_time = prefTT.getString(P.TT_TIME_MORNING, P.TT_TIME_MORNING_DEFAULT) ?: ""
        afternoon_time = prefTT.getString(P.TT_TIME_AFTERNOON, P.TT_TIME_AFTERNOON_DEFAULT) ?: ""
        evening_time = prefTT.getString(P.TT_TIME_EVENING, P.TT_TIME_EVENING_DEFAULT) ?: ""
        singleClassTime = prefTT.getInt(P.TT_TIME_CLASS, P.TT_TIME_CLASS_DEFAULT)
        val timeBreakSuperior = prefTT.getInt(P.TT_TIME_BREAK_SUPERIOR, P.TT_TIME_BREAK_SUPERIOR_DEFAULT)
        val timeBreakInferior = prefTT.getInt(P.TT_TIME_BREAK_INFERIOR, P.TT_TIME_BREAK_INFERIOR_DEFAULT)
        morningBreakTimeInferior = prefTT.getInt(P.TT_TIME_BREAK_MORNING_INFERIOR, timeBreakInferior)
        morningBreakTimeSuperior = prefTT.getInt(P.TT_TIME_BREAK_MORNING_SUPERIOR, timeBreakSuperior)
        afternoonBreakTimeInferior = prefTT.getInt(P.TT_TIME_BREAK_AFTERNOON_INFERIOR, timeBreakInferior)
        afternoonBreakTimeSuperior = prefTT.getInt(P.TT_TIME_BREAK_AFTERNOON_SUPERIOR, timeBreakSuperior)
        eveningBreakTimeInferior = prefTT.getInt(P.TT_TIME_BREAK_EVENING_INFERIOR, timeBreakInferior)
        eveningBreakTimeSuperior = prefTT.getInt(P.TT_TIME_BREAK_EVENING_SUPERIOR, timeBreakSuperior)
        appModeEnabled = prefTT.getBoolean(P.TT_APP_MODE, true)
    }

    /**
     * week indicator as a standalone iCal file
     */
    fun getWeekIndicatorComplete(context: Context, endWeek: Int): String = HEADER + getWeekIndicatorContent(context, endWeek) + FOOTER

    /**
     * week indicator as iCal events
     */
    fun getWeekIndicatorContent(context: Context, endWeek: Int): String {
        val it = StringBuilder()
        for (numWeek in 1..endWeek){
            val name = context.getString(R.string.textWeekNum, numWeek)
            it.append(HEADER_EVENT)
            it.append(getTitle(name))
            it.append(getTimeWeekIndicator(numWeek, name.hashCode().toString()))
            it.append(FOOTER_EVENT)
        }
        return it.toString()
    }

    private fun getTimeWeekIndicator(numWeek: Int, uid: String): String{
        val calendar = Calendar.getInstance()
        val presentDate = dateFormat.format(calendar.time)
        val presentTime = timeFormatSeconds.format(calendar.time)
        val re = StringBuilder()
        val dateInstance: String
        val dateEnd: String
        val amountPreDays = (numWeek - 1) * 7
        try {
            dateFormat.parse(original_date_start)?.let{ calendar.time = it }
        } catch ( e: ParseException) {
            e.printStackTrace()
        }
        calendar.add(Calendar.DATE, amountPreDays)
        dateInstance = dateFormat.format(calendar.time)
        calendar.add(Calendar.DATE, 1)
        dateEnd = dateFormat.format(calendar.time)
        val textTz = if (appModeEnabled) "" else ";TZID=Asia/Shanghai"
        val textNoTime = ";VALUE=DATE"
        re.append("\r\nUID:$uid\r\nDTSTART$textNoTime$textTz:$dateInstance\r\nDTEND$textNoTime$textTz:$dateEnd\r\nDTSTAMP$textTz:${presentDate}T$presentTime")
        return re.toString()
    }

    /**
     * from week 1 to endWeek
     */
    fun getTimeWeekly(endWeek: Int, uid: String): String{
        val calendar = Calendar.getInstance()
        val presentDate = dateFormat.format(calendar.time)
        val presentTime = timeFormatSeconds.format(calendar.time)
        val re = StringBuilder()
        val dateInstance: String
        val dateEnd: String
        try {
            dateFormat.parse(original_date_start)?.let{ calendar.time = it }
        } catch ( e: ParseException) {
            e.printStackTrace()
        }
        dateInstance = dateFormat.format(calendar.time)
        calendar.add(Calendar.DATE, 1)
        val end = dateFormat.format(calendar.time)
        val textTz = if (appModeEnabled) "" else ";TZID=Asia/Shanghai"
        val textNoTime = ";VALUE=DATE"
        re.append("\r\nUID:$uid\r\nDTSTART$textNoTime$textTz:$dateInstance\r\nDTEND$textNoTime$textTz:$end\r\nDTSTAMP$textTz:${presentDate}T$presentTime\r\n")
        //计算结束日期
        val durationDays = (endWeek - 1) * 7
        try {
            dateFormat.parse(original_date_start)?.let{ calendar.time = it }
        } catch ( e: ParseException) { e.printStackTrace() }
        calendar.add(Calendar.DATE, durationDays)
        dateEnd = dateFormat.format(calendar.time)

        re.append("RRULE:FREQ=WEEKLY;UNTIL=${dateEnd}T224400")
        return re.toString()
    }

    fun get(courses: List<CourseSingleton>): String{
        val printBuilder = StringBuilder(HEADER)
        for (course in courses) {
            for (period in course.educating.repetitions) {
                printBuilder.let {
                    course.run {
                        it.append(HEADER_EVENT)
                        it.append(getTitle(name))
                        it.append(getTime(legacyTemplate, period.fromWeek, period.toWeek, period.fortnightly,
                                educating.dayOfWeek, legacyClassPhase, legacyClassPeriod,
                                legacyLiveUid(period) + period.fromWeek.toString() + period.toWeek.toString()))
                        it.append(getLocation(educating.location))
                        it.append(getDescription(educating.educator))
                        it.append(FOOTER_EVENT).append("\r\n")
                    }
                }
            }
        }
        printBuilder.append(FOOTER)
        return printBuilder.toString()
    }

    private fun getTitle(title: String): String = "\r\nSUMMARY:$title"

    private fun getTime(
    courseTemplate: Char, weekBegin: Int, weekEnd: Int, single: Boolean,
    courseWeekDay: Int, classPhase: String?, classPeriod: Char, uid: String): String{
        if (classPhase == null) return "0"
        val calendar = Calendar.getInstance()
        val presentDate = dateFormat.format(calendar.time)
        val presentTime = timeFormatSeconds.format(calendar.time)
        val result = StringBuilder()
        //生成事件时间、重复方式、开始结束日期等

        //计算开始日期, 周一调整为周日
        val amountPreDays = ((weekBegin - 1) * 7) - 1 + courseWeekDay
        val dateInstance: String
        val dateEnd: String
        try {
            dateFormat.parse(original_date_start)?.let{ calendar.time = it }
        } catch ( e: ParseException) {
            e.printStackTrace()
        }
        calendar.add(Calendar.DATE, amountPreDays)
        dateInstance = dateFormat.format(calendar.time)
        //获取事件开始结束时间
        val classTime = when (classPhase){
            "11" -> time(calendar, TIME_MORNING, TimetablePeriod.PERIOD_ONE, courseTemplate, classPeriod)
            "12" -> time(calendar, TIME_MORNING, TimetablePeriod.PERIOD_TWO, courseTemplate, classPeriod)
            "13" -> time(calendar, TIME_MORNING, TimetablePeriod.PERIOD_THREE, courseTemplate, classPeriod)
            "21" -> time(calendar, TIME_AFTERNOON, TimetablePeriod.PERIOD_ONE, courseTemplate, classPeriod)
            "22" -> time(calendar, TIME_AFTERNOON, TimetablePeriod.PERIOD_TWO, courseTemplate, classPeriod)
            "23" -> time(calendar, TIME_AFTERNOON, TimetablePeriod.PERIOD_THREE, courseTemplate, classPeriod)
            "31" -> time(calendar, TIME_EVENING, TimetablePeriod.PERIOD_ONE, courseTemplate, classPeriod)
            "32" -> time(calendar, TIME_EVENING, TimetablePeriod.PERIOD_TWO, courseTemplate, classPeriod)
            "33" -> time(calendar, TIME_EVENING, TimetablePeriod.PERIOD_THREE, courseTemplate, classPeriod)
            else -> return "\n?#!error:1_classPhase_out_of_case"
        }
        val timeStart = timeFormat.format(calendar.time)
        calendar.add(Calendar.MINUTE, classTime)
        val timeEnd = timeFormat.format(calendar.time)

        val textTz = if (appModeEnabled) "" else ";TZID=Asia/Shanghai"
        result.append("\r\nUID:$uid\r\nDTSTART$textTz:${dateInstance}T${timeStart}00\r\nDTEND$textTz:${dateInstance}T${timeEnd}00\r\nDTSTAMP$textTz:${presentDate}T$presentTime\r\n")

        //计算结束日期, 周一调整为周日
        val amountDays = ((weekEnd - 1) * 7) - 1 + courseWeekDay
        try {
            dateFormat.parse(original_date_start)?.let{ calendar.time = it }
        } catch ( e: ParseException) { e.printStackTrace() }
        calendar.add(Calendar.DATE, amountDays)
        dateEnd = dateFormat.format(calendar.time)

        result.append("RRULE:FREQ=WEEKLY;UNTIL=${dateEnd}T224400")
        //单双周
        if (single) result.append(";INTERVAL=2")

        return result.toString()
    }

    private fun getLocation(location: String): String = "\r\nLOCATION:$location"

    private fun getDescription(description: String): String = "\r\nDESCRIPTION:$description"

    fun getTimePeriod(co: CourseSingleton, weekBegin: Int, weekEnd: Int): LongArray {
        var weekBeginning = weekBegin
        var weekEnding = weekEnd
        val courseTemplate = co.legacyTemplate
        val courseWeekDay = co.educating.dayOfWeek
        val classPhase = co.legacyClassPhase
        val classPeriod = co.legacyClassPeriod
        val calendar = Calendar.getInstance()
        //生成事件时间、重复方式、开始结束日期等
        val classTime = when (classPhase){
            "11" -> time(calendar, TIME_MORNING, TimetablePeriod.PERIOD_ONE, courseTemplate, classPeriod)
            "12" -> time(calendar, TIME_MORNING, TimetablePeriod.PERIOD_TWO, courseTemplate, classPeriod)
            "13" -> time(calendar, TIME_MORNING, TimetablePeriod.PERIOD_THREE, courseTemplate, classPeriod)
            "21" -> time(calendar, TIME_AFTERNOON, TimetablePeriod.PERIOD_ONE, courseTemplate, classPeriod)
            "22" -> time(calendar, TIME_AFTERNOON, TimetablePeriod.PERIOD_TWO, courseTemplate, classPeriod)
            "23" -> time(calendar, TIME_AFTERNOON, TimetablePeriod.PERIOD_THREE, courseTemplate, classPeriod)
            "31" -> time(calendar, TIME_EVENING, TimetablePeriod.PERIOD_ONE, courseTemplate, classPeriod)
            "32" -> time(calendar, TIME_EVENING, TimetablePeriod.PERIOD_TWO, courseTemplate, classPeriod)
            "33" -> time(calendar, TIME_EVENING, TimetablePeriod.PERIOD_THREE, courseTemplate, classPeriod)
            else -> 0
        }

        //计算开始日期
        weekBeginning--
        weekBeginning *= 7
        //周一调整为周日
        weekBeginning--
        try {
            dateFormat.parse(original_date_start)?.let{ calendar.time = it }
        } catch (e: ParseException) {
            e.printStackTrace()
        }

        calendar.add(Calendar.DATE, weekBeginning + courseWeekDay)
        val date = calendar.timeInMillis
        //获取事件开始结束时间

        val time = LongArray(3)
        time[0] = date + calendar.timeInMillis
        calendar.add(Calendar.MINUTE, classTime)
        time[1] = date + calendar.timeInMillis


        //计算结束日期
        weekEnding--
        weekEnding *= 7
        //周一调整为周日
        weekEnding--
        try {
            dateFormat.parse(original_date_start)?.let{ calendar.time = it }
        } catch (e: ParseException) {
            e.printStackTrace()
        }

        calendar.add(Calendar.DATE, weekEnding + courseWeekDay)
        time[2] = calendar.timeInMillis
        return time
    }

    private fun classTime(courseTemplate: Char, classPeriod: Char, breakTimeInferior: Int): Int{
        return when (classPeriod) {
            'a' -> if (courseTemplate == 'c')
                singleClassTime * 2 + breakTimeInferior
            else
                singleClassTime
            'b' -> singleClassTime * 2 + breakTimeInferior
            'c' -> singleClassTime * 3 + breakTimeInferior * 2
            else -> 0
        }
    }

    private fun time(calendar: Calendar, time: Int, period: Int, courseTemplate: Char, classPeriod: Char): Int{
        val keyTime: String
        val breakTimeInferior: Int
        val breakTimeSuperior: Int
        when(time){
            TIME_MORNING -> {
                keyTime = morning_time
                breakTimeInferior = morningBreakTimeInferior
                breakTimeSuperior = morningBreakTimeSuperior
            }
            TIME_AFTERNOON -> {
                keyTime = afternoon_time
                breakTimeInferior = afternoonBreakTimeInferior
                breakTimeSuperior = afternoonBreakTimeSuperior
            }
            TIME_EVENING -> {
                keyTime = evening_time
                breakTimeInferior = eveningBreakTimeInferior
                breakTimeSuperior = eveningBreakTimeSuperior
            }
            else -> return 0
        }
        // Set class starting time.
        try {
            timeFormat.parse(keyTime)?.let{ calendar.time = it }
        } catch (e: ParseException) { e.printStackTrace() }
        when(period){
            TimetablePeriod.PERIOD_ONE -> {
            }
            TimetablePeriod.PERIOD_TWO -> {
                // 与早晨/下午/晚上 开始上课时间总是相差一节大课和一个大课间的时间
                val classDuration = singleClassTime * 2 + breakTimeInferior
                calendar.add(Calendar.MINUTE, classDuration + breakTimeSuperior)
            }
            TimetablePeriod.PERIOD_THREE -> {
                // 与早晨/下午/晚上 开始上课时间总是相差两节大课和 一个大课间加一个小课间 的时间
                val classDuration = singleClassTime * 2 + breakTimeInferior
                calendar.add(Calendar.MINUTE, classDuration * 2 + breakTimeSuperior + breakTimeInferior)
            }
        }
        // 设定本节课时长，为结束时间生成做准备
        return classTime(courseTemplate, classPeriod, breakTimeInferior)
    }

    fun getUndo(eventBody: String): String = "$HEADER$HEADER_EVENT\r\nMETHOD:CANCEL\r\nSTATUS:CANCELLED\r\n$eventBody$FOOTER_EVENT$FOOTER"
}