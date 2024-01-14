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

package com.madness.collision.main

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutManager
import android.graphics.drawable.AnimatedVectorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.LocaleList
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePaddingRelative
import androidx.lifecycle.lifecycleScope
import com.madness.collision.BuildConfig
import com.madness.collision.R
import com.madness.collision.base.BaseActivity
import com.madness.collision.databinding.ActivityImmortalBinding
import com.madness.collision.diy.WindowInsets
import com.madness.collision.settings.instant.Instant
import com.madness.collision.util.*
import com.madness.collision.util.os.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

internal class ImmortalActivity : BaseActivity(), SystemBarMaintainerOwner, View.OnClickListener {
    private var logFile: File? = null
    private lateinit var viewBinding: ActivityImmortalBinding
    override val systemBarMaintainer: SystemBarMaintainer = ActivitySystemBarMaintainer(this)

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.immortalRestart -> finish()
            R.id.immortalBagShare -> actionShareLog()
            R.id.immortalBagSend -> actionIssueReport()
            R.id.immortalContactEmail -> actionEmail()
        }
    }

    private fun actionShareLog() {
        val logFile = logFile
        if (logFile == null) {
            notify(R.string.textWaitASecond)
            return
        }
        FilePop.by(this, logFile, "text/html", R.string.immortalShareTitle, imageLabel = "Boundo Log")
            .show(supportFragmentManager, FilePop.TAG)
    }

    private fun actionIssueReport() {
        val logFile = logFile
        if (logFile == null) {
            notify(R.string.textWaitASecond)
            return
        }
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
            putExtra(Intent.EXTRA_STREAM, logFile.getProviderUri(this@ImmortalActivity))
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
    }

    private fun actionEmail() {
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

    private fun getLocaleTags(): String {
        return if (OsUtils.satisfy(OsUtils.N)) LocaleList.getDefault().toLanguageTags()
        else Locale.getDefault().toLanguageTag()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("Immortal", "immortal onCreate")
        // true: from settings, false: from crash
        val isMortal = intent.getStringExtra(P.IMMORTAL_EXTRA_LAUNCH_MODE) == P.IMMORTAL_EXTRA_LAUNCH_MODE_MORTAL
        enableEdgeToEdge()
        viewBinding = ActivityImmortalBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        applyInsets()
        val context = this
        if (isMortal) viewBinding.immortalMessage.setText(R.string.textWaitASecond)
        lifecycleScope.launch(Dispatchers.Default) { produceLog(context, isMortal) }
        setupUi()
    }

    private suspend fun produceLog(context: Context, isMortal: Boolean) {
        val msgId = try {
            logFile = ImmortalLog(context).produceFile()
            if (isMortal) R.string.immortalMessageMortal else null
        } catch (e: Exception) {
            e.printStackTrace()
            R.string.text_error
        }
        msgId ?: return
        withContext(Dispatchers.Main) { viewBinding.immortalMessage.setText(msgId) }
    }

    private fun setupUi() {
        (viewBinding.immortalLogo.drawable as AnimatedVectorDrawable).start()
        viewBinding.immortalLogo.setOnLongClickListener click@{
            if (OsUtils.dissatisfy(OsUtils.N_MR1)) return@click true
            val manager = getSystemService(ShortcutManager::class.java) ?: return@click true
            Instant(this, manager).let {
                val isExisting = it.dynamicShortcuts.any { s -> s.id == P.SC_ID_IMMORTAL }
                if (isExisting) it.removeDynamicShortcuts(P.SC_ID_IMMORTAL)
                else it.addDynamicShortcuts(P.SC_ID_IMMORTAL)
            }
            notifyBriefly(R.string.text_done)
            true
        }
        viewBinding.immortalToolbar.run {
            navigationIcon = ContextCompat.getDrawable(context, R.drawable.ic_arrow_back_24)?.apply {
                setTint(ThemeUtil.getColor(context, R.attr.colorIcon))
            }
            setNavigationOnClickListener { finish() }
        }
    }

    private fun applyInsets() {
        viewBinding.immortalRoot.setOnApplyWindowInsetsListener { v, insets ->
            if (checkInsets(insets)) edgeToEdge(insets, false)
            val isRtl = if (v.isLayoutDirectionResolved) v.layoutDirection == View.LAYOUT_DIRECTION_RTL else false
            consumeInsets(WindowInsets(insets, isRtl))
            WindowInsetsCompat.CONSUMED.toWindowInsets()!!
        }
    }

    private fun consumeInsets(insets: WindowInsets) {
        mainApplication.insetTop = insets.top
        mainApplication.insetBottom = insets.bottom
        mainApplication.insetStart = insets.start
        mainApplication.insetEnd = insets.end
        viewBinding.immortalRoot.updatePaddingRelative(start = insets.start, end = insets.end)
        viewBinding.immortalToolbar.run {
            updatePaddingRelative(top = insets.top)
            measure()
            viewBinding.immortalContent.updatePaddingRelative(top = measuredHeight)
        }
    }
}
