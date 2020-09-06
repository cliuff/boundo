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

package com.madness.collision.main

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.AnimatedVectorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.SpannedString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import com.madness.collision.BuildConfig
import com.madness.collision.R
import com.madness.collision.diy.WindowInsets
import com.madness.collision.instant.Instant
import com.madness.collision.settings.SettingsFunc
import com.madness.collision.util.*
import kotlinx.android.synthetic.main.activity_immortal.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

internal class ImmortalActivity : AppCompatActivity(), View.OnClickListener {

    companion object {
        private val COLOR_RED = Color.parseColor("#FFF03030")
        private val COLOR_ORANGE = Color.parseColor("#FFF0A070")
        private val COLOR_GREEN = Color.parseColor("#FF10A070")
        private val COLOR_BLUE = Color.parseColor("#FF1070A0")
    }

    private var logFile: File? = null

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.immortalRestart -> finish()

            R.id.immortalBagShare -> {
                if (logFile != null){
                    supportFragmentManager.let {
                        FilePop.by(this, logFile!!, "text/html", R.string.immortalShareTitle).show(it, FilePop.TAG)
                    }
                } else {
                    notify(R.string.textWaitASecond)
                }
            }

            R.id.immortalBagSend -> {
                if (logFile != null){
                    val title = getString(R.string.immortalSendTitle)
                    val locales = getLocaleTags()
                    val body = "\n\nApp: ${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})\nLocales: $locales\n\n"
                    val intent = Intent().apply {
                        // ACTION_SENDTO (for no attachment) or
                        // ACTION_SEND (for one attachment) or
                        // ACTION_SEND_MULTIPLE (for multiple attachments)
                        action = Intent.ACTION_SEND
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(P.CONTACT_EMAIL))
                        putExtra(Intent.EXTRA_STREAM, logFile!!.getProviderUri(this@ImmortalActivity))
                        putExtra(Intent.EXTRA_SUBJECT, getString(R.string.immortalEmailSubject))
                        type = "message/rfc822"
                        putExtra(Intent.EXTRA_TEXT, body)
                        // android 10 title
                        putExtra(Intent.EXTRA_TITLE, title)
                    }
                    try {
                        startActivity(intent)
                        X.toast(this, R.string.textEmail, Toast.LENGTH_LONG)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        notify(R.string.text_app_not_installed)
                    }
                } else {
                    notify(R.string.textWaitASecond)
                }
            }

            R.id.immortalContactEmail -> {
                val intent = Intent().apply {
                    action = Intent.ACTION_SENDTO
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    data = Uri.parse("mailto:" + P.CONTACT_EMAIL)
                }
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                    CollisionDialog.infoCopyable(this, P.CONTACT_EMAIL).show()
                }
            }

            R.id.immortalContactQQ -> {
                val intent = Intent().apply {
                    action = Intent.ACTION_VIEW
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    data = Uri.parse("mqqwpa://im/chat?chat_type=wpa&uin=${P.CONTACT_QQ}&version=1")
                }
                try {
                    startActivity(intent)
                    X.toast(this, R.string.Advice_QQ_Toast_Text, Toast.LENGTH_LONG)
                } catch (e: Exception) {
                    e.printStackTrace()
                    CollisionDialog.infoCopyable(this, P.CONTACT_QQ).show()
                }
            }
        }
    }

    private fun getLocaleTags(): String {
        return if (X.aboveOn(X.N)) LocaleList.getDefault().toLanguageTags() else Locale.getDefault().toLanguageTag()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("Immortal", "immortal onCreate")
        // true: from settings, false: from crash
        val isMortal = intent.getStringExtra(P.IMMORTAL_EXTRA_LAUNCH_MODE) == P.IMMORTAL_EXTRA_LAUNCH_MODE_MORTAL
        ThemeUtil.updateTheme(this, getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE))
        window?.let { SystemUtil.applyEdge2Edge(it) }
        SettingsFunc.updateLanguage(this)
        setContentView(R.layout.activity_immortal)
        applyInsets()
        window?.let {
            val darkBar = mainApplication.isPaleTheme
            SystemUtil.applyStatusBarColor(this, it, darkBar, true)
            SystemUtil.applyNavBarColor(this, it, darkBar, true)
        }
        if (isMortal) immortalMessage.setText(R.string.textWaitASecond)
        GlobalScope.launch {
            try {
                logFile = log()
                launch(Dispatchers.Main) {
                    if (isMortal) immortalMessage.setText(R.string.immortalMessageMortal)
                }
            } catch (e: Exception){
                e.printStackTrace()
                launch(Dispatchers.Main) {
                    immortalMessage.setText(R.string.text_error)
                }
            }
        }
        (immortalLogo.drawable as AnimatedVectorDrawable).start()
        immortalLogo.setOnLongClickListener {
            if (X.belowOff(X.N_MR1)) return@setOnLongClickListener true
            val manager = getSystemService(ShortcutManager::class.java) ?: return@setOnLongClickListener true
            Instant(this, manager).let {
                val isExisting = it.dynamicShortcuts.find { s ->
                    s.id == P.SC_ID_IMMORTAL
                } != null
                if (isExisting) it.removeDynamicShortcuts(P.SC_ID_IMMORTAL)
                else it.addDynamicShortcuts(P.SC_ID_IMMORTAL)
            }
            notifyBriefly(R.string.text_done)
            true
        }
    }

    private fun applyInsets(){
        immortalRoot.setOnApplyWindowInsetsListener { _, insets ->
            consumeInsets(WindowInsets(insets))
            insets
        }
    }

    private fun consumeInsets(insets: WindowInsets) {
        mainApplication.insetTop = insets.top
        mainApplication.insetBottom = insets.bottom
        mainApplication.insetLeft = insets.left
        mainApplication.insetRight = insets.right
        immortalRoot.alterPadding(start = insets.left, end = insets.right)
    }

    private fun log(): File {
        // - device info
        val manufacture = Build.MANUFACTURER
        val model = Build.MODEL
        val product = Build.PRODUCT
        val device = Build.DEVICE
        // - device model info

        // - app version info
//        val packageInfo = MiscApp.getPackageInfo(this, packageName = packageName) ?: return File("")
//        val ver = PackageInfoCompat.getLongVersionCode(packageInfo).toString()
        val ver = BuildConfig.VERSION_CODE
        val verName = BuildConfig.VERSION_NAME
        // - app version info

        val locales = getLocaleTags()

        val apiLevel = Build.VERSION.SDK_INT

        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", SystemUtil.getLocaleApp()).format(Calendar.getInstance().time)
        val logFile = F.createFile(
                F.cachePublicPath(this),
                P.DIR_NAME_LOG,
                "Boundo $verName($ver) @$time $manufacture $model $apiLevel.html"
        )

        // - clear extra log files
        logFile.parentFile?.deleteRecursively()

        if (!F.prepare4(logFile)) {
            throw Exception("Error occurred while preparing file")
        }
        val writer = FileWriter(logFile)
        writer.write(wrapInHtml("Manufacture: $manufacture\nModel: $model\nProduct: $product\nDevice: $device\nAPI: $apiLevel\nApp: $verName($ver)\nLocales: $locales\n"))

        if (X.aboveOn(X.R)) {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
            val exitReasons = am?.getHistoricalProcessExitReasons(BuildConfig.APPLICATION_ID, 0, 0) ?: emptyList()
            if (exitReasons.isNotEmpty()) {
                val reasonsTitle = SpannableString("Historical process exit reasons")
                reasonsTitle.setSpan(StyleSpan(Typeface.BOLD), 0, reasonsTitle.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                writer.write(reasonsTitle.toString().wrappedInHtml)
            }
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
                val info = "Process name: ${exitInfo.processName}(pid ${exitInfo.pid})\nReason: $reason\nDesc: ${exitInfo.description ?: "Unspecified"}\n"
                writer.write(info.wrappedInHtml)
                exitInfo.traceInputStream?.let { traceStream ->
                    val reader = InputStreamReader(traceStream)
                    reader.useLines { it.forEach {  line -> writer.write(wrapInHtml(line)) } }
                    writer.write("\n")
                }
            }
        }
        // - write logcat
        val process = Runtime.getRuntime().exec("logcat -d -v time")
        val reader = InputStreamReader(process.inputStream)
        reader.useLines { it.forEach {  line -> writer.write(wrapInHtml(line)) } }
        // - write logcat

        writer.close()

        Log.i("Immortal", "immortal log: ${logFile.path}")

        return logFile
    }

    private val String.wrappedInHtml: String
        get() = wrapInHtml(this)

    private fun wrapInHtml(line: String): String {
        val re = SpannableStringBuilder(line)
        when {
            re.matches("[\\d- :.]*E/.*".toRegex()) -> re.highlightAll(COLOR_RED)
            re.matches("[\\d- :.]*W/.*".toRegex()) -> re.highlightAll(COLOR_ORANGE)
            re.matches(".*beginning of crash.*".toRegex()) -> re.highlightAll(COLOR_GREEN)
            re.matches(".*System\\.exit called.*".toRegex()) -> re.highlightAll(COLOR_GREEN)
            re.matches(".*VM exiting.*".toRegex()) -> re.highlightAll(COLOR_GREEN)
            re.matches(".*immortal onCreate.*".toRegex()) -> re.highlightAll(COLOR_GREEN)
            re.matches(".*immortal log.*".toRegex()) -> re.highlightAll(COLOR_GREEN)
        }
        if (re.matches("[\\d- :.]*./dness.collisio.*".toRegex())) re.highlight("dness.collisio", COLOR_BLUE)
        if (BuildConfig.DEBUG && re.matches("[\\d- :.]*./ollision.morta.*".toRegex())) re.highlight("ollision.morta", COLOR_BLUE)
        re.highlight(BuildConfig.BUILD_PACKAGE, COLOR_BLUE)
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
