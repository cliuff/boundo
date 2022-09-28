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

package com.madness.collision.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.Toolbar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Code
import androidx.compose.material.icons.twotone.Email
import androidx.compose.material3.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.edit
import androidx.fragment.app.activityViewModels
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.madness.collision.Democratic
import com.madness.collision.R
import com.madness.collision.main.MainViewModel
import com.madness.collision.util.*
import com.madness.collision.util.os.OsUtils

internal class AdviceFragment : TaggedFragment(), Democratic {
    override val category: String = "Advice"
    override val id: String = "Advice"

    private val mainViewModel: MainViewModel by activityViewModels()
    private var mutableComposeView: ComposeView? = null
    private val composeView: ComposeView get() = mutableComposeView!!

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        mainViewModel.configNavigation(toolbar, iconColor)
        toolbar.setTitle(R.string.Main_TextView_Advice_Text)
        return true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        mutableComposeView = ComposeView(inflater.context)
        return composeView
    }

    override fun onDestroyView() {
        mutableComposeView = null
        super.onDestroyView()
    }

    private val @receiver:DrawableRes Int.icon get() = AboutOptionIcon.Res(this)
    private val ImageVector.icon get() = AboutOptionIcon.Vector(this)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        democratize(mainViewModel)
        val context = context ?: return
        val options = listOf(
            AboutOption(Icons.TwoTone.Code.icon, getString(R.string.advice_license), "", false) {
                startActivity(Intent(context, OssLicensesMenuActivity::class.java))
            },
            AboutOption(R.drawable.ic_github_24.icon, "Github", "cliuff/boundo", true) {
                openUrl(context, P.LINK_SOURCE_CODE)
            },
            AboutOption(Icons.TwoTone.Email.icon, "Email", P.CONTACT_EMAIL, true) {
                openEmail(context, P.CONTACT_EMAIL)
            },
            AboutOption(R.drawable.ic_twitter_24.icon, "Twitter", getString(R.string.about_twitter_account), true) {
                openUrl(context, P.LINK_TWITTER_ACCOUNT)
            },
            AboutOption(R.drawable.ic_telegram_24.icon, "Telegram", "t.me/cliuff_boundo", true) {
                openUrl(context, P.LINK_TELEGRAM_GROUP)
            },
        )
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        val colorScheme = if (OsUtils.satisfy(OsUtils.S)) {
            if (mainApplication.isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            if (mainApplication.isDarkTheme) darkColorScheme() else lightColorScheme()
        }
        composeView.setContent {
            MaterialTheme(colorScheme = colorScheme) {
                AboutPage(mainViewModel, options = options)
            }
        }
    }

    private fun openUrl(context: Context, url: String) {
        val intent = Intent().apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            data = Uri.parse(url)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            CollisionDialog.infoCopyable(context, url).show()
        }
    }

    private fun openEmail(context: Context, address: String) {
        val intent = Intent().apply {
            action = Intent.ACTION_SENDTO
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            data = Uri.parse("mailto:$address")
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            CollisionDialog.infoCopyable(context, address).show()
        }
    }

    private fun debugMode(context: Context) = object : View.OnClickListener {
        private var count4DebugMode = 0
        override fun onClick(v: View?) {
            count4DebugMode++
            if (count4DebugMode != 6) return
            count4DebugMode = 0
            val app = mainApplication
            app.debug = !app.debug
            val pref = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
            pref.edit { putBoolean(P.ADVANCED, app.debug) }
            val switchMsg = if (app.debug) "ON" else "OFF"
            X.toast(context, getString(R.string.Advice_Switch_Debug_Text) + " $switchMsg", Toast.LENGTH_LONG)
        }
    }
}