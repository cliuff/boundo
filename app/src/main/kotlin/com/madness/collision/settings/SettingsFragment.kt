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
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.res.use
import com.madness.collision.Democratic
import com.madness.collision.R
import com.madness.collision.chief.app.ComposeFragment
import com.madness.collision.chief.app.rememberColorScheme
import com.madness.collision.main.MainActivity
import com.madness.collision.util.P
import com.madness.collision.util.PopupUtil
import com.madness.collision.util.config.LocaleUtils
import com.madness.collision.util.os.OsUtils
import java.util.Locale

internal class SettingsFragment : ComposeFragment(), Democratic {
    override val category: String = "Settings"
    override val id: String = "Settings"

    companion object {
        private const val TAG = "Settings"
    }

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        mainViewModel.configNavigation(toolbar, iconColor)
        toolbar.setTitle(R.string.Main_ToolBar_title_Settings)
        return true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        democratize(mainViewModel)
    }

    @Composable
    override fun ComposeContent() {
        val context = LocalContext.current
        MaterialTheme(colorScheme = rememberColorScheme()) {
            SettingsPage(
                mainViewModel = mainViewModel,
                paddingValues = rememberContentPadding(),
                showLanguages = { showLanguages(context) },
            )
        }
    }

    // get lang_country only: zh_Hant_TW -> zh_TW
    private fun Locale.toRegionalString(): String {
        if (language.isEmpty()) return ""
        return arrayOf(language, country).filterNot { it.isEmpty() }.joinToString(separator = "_")
    }

    private fun showLanguages(context: Context) {
        val langEntries = context.resources.obtainTypedArray(R.array.prefSettingsLangEntries)
        val langValues = context.resources.obtainTypedArray(R.array.prefSettingsLangValues)
        val lang = LocaleUtils.getSet()?.first()?.toRegionalString() ?: LanguageMan.AUTO
        Log.i(TAG, "Currently set: $lang")
        val langIndex = P.getPrefIndex(lang, langValues)
        PopupUtil.selectSingle(context, R.string.Settings_Language_Dialog_Title, langEntries, langIndex) {
            pop, _, index ->
            pop.dismiss()
            val values = context.resources.obtainTypedArray(R.array.prefSettingsLangValues)
            val newLang = values.use { it.getString(index) } ?: LanguageMan.AUTO
            val newLocale = LanguageMan.getLocale(newLang)
            val systemAppLang = LocaleUtils.getApp()[0].toString()
            Log.i(TAG, "New lang: $newLang, system app lang: $systemAppLang")

            val oldLangTag = LocaleUtils.getRuntimeFirst().toLanguageTag()
            LocaleUtils.set(newLocale)
            val newLangTag = LocaleUtils.getRuntimeFirst().toLanguageTag()
            val shouldSwitch = OsUtils.dissatisfy(OsUtils.T) && oldLangTag != newLangTag
            if (shouldSwitch) {
                mainViewModel.action.value = MainActivity.ACTION_RECREATE to null
            }
        }.show()
    }
}
