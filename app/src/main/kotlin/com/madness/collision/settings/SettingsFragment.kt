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
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.appcompat.widget.Toolbar
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.edit
import androidx.core.content.res.use
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import com.madness.collision.Democratic
import com.madness.collision.R
import com.madness.collision.main.MainActivity
import com.madness.collision.main.MainViewModel
import com.madness.collision.util.*
import com.madness.collision.util.AppUtils.asBottomMargin
import com.madness.collision.util.os.OsUtils

internal class SettingsFragment : TaggedFragment(), Democratic {

    override val category: String = "Settings"
    override val id: String = "Settings"

    companion object {
        private const val TAG = "Settings"
    }

    private val mainViewModel: MainViewModel by activityViewModels()
    private var _composeView: ComposeView? = null
    private val composeView: ComposeView get() = _composeView!!

    private lateinit var prHandler: PermissionRequestHandler

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        mainViewModel.configNavigation(toolbar, iconColor)
        toolbar.setTitle(R.string.Main_ToolBar_title_Settings)
        return true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _composeView = ComposeView(inflater.context)
        return composeView
    }

    override fun onDestroyView() {
        _composeView = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        democratize(mainViewModel)
        mainViewModel.contentWidthTop.observe(viewLifecycleOwner) {
            composeView.updatePadding(top = it)
        }
        mainViewModel.contentWidthBottom.observe(viewLifecycleOwner) {
            composeView.updatePadding(bottom = asBottomMargin(it))
        }

        val context = context ?: return
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        val colorScheme = if (mainApplication.isDarkTheme) {
            if (OsUtils.satisfy(OsUtils.S)) dynamicDarkColorScheme(context) else darkColorScheme()
        } else {
            if (OsUtils.satisfy(OsUtils.S)) dynamicLightColorScheme(context)
            else lightColorScheme(primary = Color(0xff572ad2), primaryContainer = Color(0xfff9f0f7))
        }
        composeView.setContent {
            MaterialTheme(colorScheme = colorScheme) {
                SettingsPage(mainViewModel) { showLanguages(it) }
            }
        }
    }

    private fun showLanguages(context: Context) {
        val langEntries = context.resources.obtainTypedArray(R.array.prefSettingsLangEntries)
        val langValues = context.resources.obtainTypedArray(R.array.prefSettingsLangValues)
        val langMan = LanguageMan(context)
        val lang = langMan.getLanguage()
        Log.i(TAG, "Currently set: $lang")
        val langIndex = P.getPrefIndex(lang, langValues)
        PopupUtil.selectSingle(context, R.string.Settings_Language_Dialog_Title, langEntries, langIndex) {
            pop, _, index ->
            pop.dismiss()
            val values = context.resources.obtainTypedArray(R.array.prefSettingsLangValues)
            val newLang = values.use { it.getString(index) } ?: LanguageMan.AUTO
            val systemAppLang = SystemUtil.getLocaleApp().toString()
            Log.i(TAG, "New lang: $newLang, system app lang: $systemAppLang")
            val oldLocaleString = SystemUtil.getLocaleUsr(context).toString()
            langMan.setLanguage(newLang)
            val newLocaleString = SystemUtil.getLocaleUsr(context).toString()
            val shouldSwitch = oldLocaleString != newLocaleString
            if (shouldSwitch) {
                mainViewModel.action.value = MainActivity.ACTION_RECREATE to null
            }
        }.show()
    }

    // check update from CoolApk, not used now, need INTERNET permission
    private fun checkUpdate(context: Context){
        val activity = activity ?: return
        val bar = ProgressBar(context)
        val checking = CollisionDialog.loading(context, bar)
        bar.post {
            val settingsPreferences = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
            settingsPreferences.edit {
                putBoolean(P.SETTINGS_UPDATE_NOTIFY, true)
                putBoolean(P.SETTINGS_UPDATE_VIA_SETTINGS, true)
            }
            prHandler = PermissionRequestHandler(activity)
            SettingsFunc.check4Update(context, checking, prHandler)
        }
        checking.show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode){
            SettingsFunc.requestWriteStorage -> {
                if (grantResults.isEmpty()) return
                when (grantResults[0]){
                    PackageManager.PERMISSION_GRANTED -> prHandler.resumeJob.run()
                }
            }
        }
    }

}
