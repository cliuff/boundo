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
import android.content.SharedPreferences
import androidx.core.content.edit
import com.madness.collision.util.P
import com.madness.collision.util.SystemUtil
import java.util.*

class LanguageMan(private val context: Context) {
    companion object {
        const val AUTO = "auto"

        fun getLocaleContext(context: Context, lang: String): Context {
            if (lang == AUTO) return context
            return SystemUtil.getLocaleContext(context, SystemUtil.toLocale(lang))
        }
    }

    private val pref: SharedPreferences by lazy {
        context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
    }

    fun getLanguage(): String {
        return pref.getString(P.SETTINGS_LANGUAGE, AUTO) ?: AUTO
    }

    fun setLanguage(language: String) {
        pref.edit { putString(P.SETTINGS_LANGUAGE, language) }
    }

    fun getLocale(): Locale {
        return SystemUtil.toLocale(getLanguage())
    }

    fun getLocaleContext(): Context {
        return getLocaleContext(context, getLanguage())
    }
}