package com.madness.collision.unit.school_timetable.parser

import android.content.Context
import android.widget.Toast
import com.madness.collision.R
import com.madness.collision.unit.school_timetable.data.Timetable
import com.madness.collision.util.X
import com.madness.collision.unit.school_timetable.R as MyR

internal class Parser(private val context: Context) {
    lateinit var timetable: Timetable

    /**
     * Load and parse clipboard content into file
     */
    fun parseFromClipboard(): Boolean {
        var re = false
        try {
            re = clipboardToFilePrivate(context)
        } catch (e: Exception) {
            e.printStackTrace()
            X.toast(context, R.string.text_error, Toast.LENGTH_SHORT)
        }
        return re
    }

    private fun clipboardToFilePrivate(context: Context): Boolean {
        val tableParser = TableParser(context)
        if (tableParser.isLegacyUnsupportedBrowser) {
            X.toast(context, MyR.string.ics_Function_Toast_NOHtml_Text, Toast.LENGTH_LONG)
            return false
        }
        if (!tableParser.isLegacySuccessfullyConstructed) return false
        val clipboardContent = tableParser.legacyContent
        val re = TimetableParser.parseRawTable(clipboardContent)
        when (re.re) {
            TimetableParser.ERROR_NO_CONTENT, "" -> {
                X.toast(context, R.string.text_no_content, Toast.LENGTH_LONG)
                return false
            }
            TimetableParser.ERROR_UNDEFINED -> return false
        }
        timetable = TimetableParser.resolveStandard(context, re)
        return true
    }
}
