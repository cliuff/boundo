package com.madness.collision.unit.school_timetable.calendar

import android.content.Context
import android.net.Uri
import com.madness.collision.unit.school_timetable.data.CourseSingleton

class Time4Calendar {
    var time: LongArray = LongArray(0)
    var week: String = ""
    var single: Boolean = false
}

fun getTime4Calendar(context: Context, course: CourseSingleton): List<Time4Calendar> {
    ICal.loadTime(context)
    val t4cs = ArrayList<Time4Calendar>()
    for (repetition in course.educating.repetitions) {
        val t4c = Time4Calendar()
        ICal.getTimePeriod(course, repetition.fromWeek, repetition.toWeek)
        t4c.time = ICal.getTimePeriod(course, repetition.fromWeek, repetition.toWeek)
        t4c.week = repetition.fromWeek.toString() + repetition.toWeek.toString()
        t4c.single = repetition.fortnightly
        t4cs.add(t4c)
    }
    return t4cs
}

class CustomizedCalendar(var name: String, var id: String)

fun getCalendar(context: Context): List<CustomizedCalendar> {
    val mCalendars = ArrayList<CustomizedCalendar>()
    val projection = arrayOf("_id", "calendar_displayName")
    val calendars = Uri.parse("content://com.android.calendar/calendars")

    val contentResolver = context.contentResolver
    val managedCursor = contentResolver.query(calendars, projection, null, null, null)

    if (managedCursor != null && managedCursor.moveToFirst()) {
        var calName: String
        var calID: String
        val nameCol = managedCursor.getColumnIndex(projection[1])
        val idCol = managedCursor.getColumnIndex(projection[0])
        do {
            calName = managedCursor.getString(nameCol)
            calID = managedCursor.getString(idCol)
            mCalendars.add(CustomizedCalendar(calName, calID))
        } while (managedCursor.moveToNext())
        managedCursor.close()
    }
    return mCalendars
}
