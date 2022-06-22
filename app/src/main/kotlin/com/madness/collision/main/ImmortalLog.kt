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

package com.madness.collision.main

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.LocaleList
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.SpannedString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.text.HtmlCompat
import com.jaredrummler.android.device.DeviceName
import com.madness.collision.BuildConfig
import com.madness.collision.util.F
import com.madness.collision.util.P
import com.madness.collision.util.os.OsUtils
import com.madness.collision.util.ui.appLocale
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

class ImmortalLog(private val context: Context) {
    private val colorRed = Color.parseColor("#FFF03030")
    private val colorOrange = Color.parseColor("#FFF0A070")
    private val colorGreen = Color.parseColor("#FF10A070")
    private val colorBlue = Color.parseColor("#FF1070A0")
    private val manufacture = Build.MANUFACTURER
    private val ver = BuildConfig.VERSION_CODE
    private val verName = BuildConfig.VERSION_NAME
    private val apiLevel = Build.VERSION.SDK_INT
    init { DeviceName.init(context) }
    private val deviceName by lazy { DeviceName.getDeviceName() }

    fun produceFile(): File {
        // colon(:) is illegal when used in file name in external download folder on Android 11+
        // (while in app files dir is fine, due to MediaStore (database)?)
        val time = SimpleDateFormat("yyyyMMdd'T'HHmm", appLocale).format(Calendar.getInstance().time)
        val fileName = "Boundo $verName($ver) $time $manufacture $deviceName $apiLevel.html"
        val logFile = F.createFile(F.cachePublicPath(context), P.DIR_NAME_LOG, fileName)
        // - clear extra log files
        logFile.parentFile?.deleteRecursively()
        if (!F.prepare4(logFile)) throw Exception("Error occurred while preparing file")
        FileWriter(logFile).use { writeInfo(it) }
        Log.i("Immortal", "Immortal log: ${logFile.path}")
        return logFile
    }

    private fun writeInfo(writer: FileWriter) {
        writer.write(wrapInHtml("$manufacture $deviceName"))
        listOf(
            // device info
            "Manufacture" to manufacture,
            "Model" to Build.MODEL,
            "Product" to Build.PRODUCT,
            "Device(Code name)" to Build.DEVICE,
            // other info
            "API level" to apiLevel.toString(),
            "App version" to "$verName($ver)",
            "Locales" to getLocaleTags(),
        ).run {
            listOf(slice(0..3), slice(4..lastIndex))
        }.forEach {
            it.joinToString(separator = "\n") { p -> "${p.first}: ${p.second}" }.let { s ->
                writer.write(s.wrappedInHtml)
            }
        }
        writer.write("".wrappedInHtml)

        if (OsUtils.satisfy(OsUtils.R)) writeExitReasons(writer)

        // - write logcat
        val process = Runtime.getRuntime().exec("logcat -d -v time")
        val reader = InputStreamReader(process.inputStream)
        reader.useLines { it.forEach {  line -> writer.write(wrapInHtml(line)) } }
    }

    private fun getLocaleTags(): String {
        return if (OsUtils.satisfy(OsUtils.N)) LocaleList.getDefault().toLanguageTags()
        else Locale.getDefault().toLanguageTag()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun writeExitReasons(writer: FileWriter) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager? ?: return
        val exitReasons = am.getHistoricalProcessExitReasons(BuildConfig.APPLICATION_ID, 0, 0)
        if (exitReasons.isEmpty()) return
        val reasonsTitle = SpannableString("Historical process exit reasons")
        reasonsTitle.setSpan(StyleSpan(Typeface.BOLD), 0, reasonsTitle.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        writer.write(reasonsTitle.toString().wrappedInHtml)
        exitReasons.forEach { exitInfo ->
            val reason = when (exitInfo.reason) {
                ApplicationExitInfo.REASON_ANR -> "ANR"
                ApplicationExitInfo.REASON_CRASH -> "crash"
                ApplicationExitInfo.REASON_CRASH_NATIVE -> "crash_native"
                ApplicationExitInfo.REASON_DEPENDENCY_DIED -> "dependency_died"
                ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "excessive_resource_usage"
                ApplicationExitInfo.REASON_EXIT_SELF -> "exit_self"
                ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "initialization_failure"
                ApplicationExitInfo.REASON_LOW_MEMORY -> "low_memory"
                ApplicationExitInfo.REASON_OTHER -> "other"
                ApplicationExitInfo.REASON_PERMISSION_CHANGE -> "permission_change"
                ApplicationExitInfo.REASON_SIGNALED -> "signaled"
                ApplicationExitInfo.REASON_UNKNOWN -> "unknown"
                ApplicationExitInfo.REASON_USER_REQUESTED -> "user_requested"
                ApplicationExitInfo.REASON_USER_STOPPED -> "user_stopped"
                else -> "unspecified"
            }
            val info = "Process name: ${exitInfo.processName}(pid ${exitInfo.pid})\nReason: $reason\nDesc: ${exitInfo.description ?: "Unspecified"}"
            writer.write(info.wrappedInHtml)
            exitInfo.traceInputStream?.let { traceStream ->
                val reader = InputStreamReader(traceStream)
                reader.useLines { it.forEach {  line -> writer.write(wrapInHtml(line)) } }
            }
        }
        writer.write("".wrappedInHtml)
    }

    private val String.wrappedInHtml: String
        get() = wrapInHtml(this)

    private fun wrapInHtml(line: String): String {
        val re = SpannableStringBuilder(line)
        when {
            re.matches("[\\d- :.]*E/.*".toRegex()) -> re.highlightAll(colorRed)
            re.matches("[\\d- :.]*W/.*".toRegex()) -> re.highlightAll(colorOrange)
            re.matches(".*beginning of crash.*".toRegex()) -> re.highlightAll(colorGreen)
            re.matches(".*System\\.exit called.*".toRegex()) -> re.highlightAll(colorGreen)
            re.matches(".*VM exiting.*".toRegex()) -> re.highlightAll(colorGreen)
            re.matches(".*immortal onCreate.*".toRegex()) -> re.highlightAll(colorGreen)
            re.matches(".*immortal log.*".toRegex()) -> re.highlightAll(colorGreen)
        }
        if (re.matches("[\\d- :.]*./dness.collisio.*".toRegex())) re.highlight("dness.collisio", colorBlue)
        if (BuildConfig.DEBUG && re.matches("[\\d- :.]*./ollision.morta.*".toRegex())) re.highlight("ollision.morta", colorBlue)
        re.highlight(BuildConfig.BUILD_PACKAGE, colorBlue)
        re.appendLine()
        return HtmlCompat.toHtml(SpannedString(re), HtmlCompat.TO_HTML_PARAGRAPH_LINES_INDIVIDUAL)
    }

    private fun SpannableStringBuilder.highlightAll(color: Int) {
        setSpan(ForegroundColorSpan(color), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun SpannableStringBuilder.highlight(content: String, color: Int) {
        var indexEnd = 0
        while (true) {
            val indexStart = indexOf(content, indexEnd)
            if (indexStart == -1) break
            indexEnd = indexStart + content.length
            setSpan(ForegroundColorSpan(color), indexStart, indexEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
}
