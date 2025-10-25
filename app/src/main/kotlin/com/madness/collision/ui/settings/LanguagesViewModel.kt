/*
 * Copyright 2025 Clifford Liu
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

package com.madness.collision.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madness.collision.main.MainActivity
import com.madness.collision.settings.LanguageMan
import com.madness.collision.util.config.LocaleUtils
import com.madness.collision.util.mainApplication
import com.madness.collision.util.os.OsUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

class LanguagesViewModel : ViewModel() {
    private val mutUiState: MutableStateFlow<LanguagesUiState>
    val uiState: MutableStateFlow<LanguagesUiState>

    private var initJob: Job? = null

    init {
        val state = LanguagesUiState(selectedLanguage = "")
        mutUiState = MutableStateFlow(state)
        uiState = mutUiState
    }

    fun init() {
        if (initJob != null) return
        initJob = viewModelScope.launch(Dispatchers.Default) {
            mutUiState.update { loadState() }
        }
    }

    private fun loadState(): LanguagesUiState {
        val lang = LocaleUtils.getSet()?.first()?.toRegionalString() ?: LanguageMan.AUTO
        return LanguagesUiState(selectedLanguage = lang)
            .also { Log.i("LanguageSetting", "Currently set: $lang") }
    }

    fun selectLanguage(languageCode: String) {
        mutUiState.update { it.copy(selectedLanguage = languageCode) }

        viewModelScope.launch(Dispatchers.Default) {
            switchLanguage(languageCode)
        }
    }
}

// get lang_country only: zh_Hant_TW -> zh_TW
private fun Locale.toRegionalString(): String {
    if (language.isEmpty()) return ""
    // use legacy language codes on Android 15+
    val lang = mapOf("he" to "iw", "yi" to "ji", "id" to "in")[language] ?: language
    return listOfNotNull(lang, country.ifEmpty { null }).joinToString(separator = "_")
}

private fun switchLanguage(newLang: String) {
    val newLocale = LanguageMan.getLocale(newLang)
    val systemAppLang = LocaleUtils.getApp()[0].toString()
    Log.i("LanguageSetting", "New lang: $newLang, system app lang: $systemAppLang")

    // preserve current language tag before setting new locale
    val oldLangTag = LocaleUtils.getRuntimeFirst().toLanguageTag()
    LocaleUtils.set(newLocale)

    if (!OsUtils.satisfy(OsUtils.T)) {
        val newLangTag = LocaleUtils.getRuntimeFirst().toLanguageTag()
        if (oldLangTag != newLangTag) {
            mainApplication.setAction(MainActivity.ACTION_RECREATE to null)
        }
    }
}
