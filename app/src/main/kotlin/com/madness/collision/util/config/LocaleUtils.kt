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

package com.madness.collision.util.config

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import androidx.core.os.LocaleListCompat
import com.madness.collision.settings.LanguageMan
import com.madness.collision.util.SystemUtil
import com.madness.collision.util.os.OsUtils
import com.madness.collision.util.ui.appContext
import java.util.*

object LocaleUtils {
    // IETF language tags: lang-country
    private val supportedTagSet = hashSetOf(
        "en", "en-GB", "en-US",
        "zh", "zh-CN", "zh-HK", "zh-MO", "zh-TW", "zh-SG",
        "ru", "ru-RU", "uk", "uk-UA", "es", "es-ES", "es-US",
        "ar", "it", "it-IT", "pt", "pt-PT",
        "th", "th-TH", "vi", "vi-VN",
        "fr", "fr-FR", "el", "el-GR",
        "ja", "ja-JP", "ko", "ko-KR",
        "tr", "tr-TR", "de", "de-DE",
        "bn", "bn-BD", "fa", "fa-AF", "fa-IR",
        "hi", "hi-IN", "in", "in-ID",
        "mr", "mr-IN", "pa", "pa-PK",
        "pl", "pl-PL",
    )
    private var supportedLocales: List<Locale>? = null

    @RequiresApi(OsUtils.N)
    private fun LocaleList.toList() = (0 until size()).mapNotNull { get(it) }

    // Settings
    fun getSystem(): List<Locale> {
        // Resources.getSystem() is not working (at least for 13)
//        val config = Resources.getSystem().configuration
//        val locales = ConfigurationCompat.getLocales(config)
        val list = kotlin.run l@{
            if (OsUtils.dissatisfy(OsUtils.T)) return@l null
            val localeMan = appContext.getSystemService<LocaleManager>() ?: return@l null
            LocaleListCompat.wrap(localeMan.systemLocales)
        }
        val locales = list ?: LocaleListCompat.getDefault()
        return (0 until locales.size()).mapNotNull { locales[it] }
    }

    fun getSupported(): List<Locale> {
        var list = supportedLocales
        if (list != null) return list
        list = supportedTagSet.map { Locale.forLanguageTag(it) }
        supportedLocales = list
        return list
    }

    fun getApp(): List<Locale> {
        val list = getSystem()
            .filter { it.toRegionalTag() in supportedTagSet || it.language in supportedTagSet }
        return list.ifEmpty { listOf(Locale.ENGLISH) }
    }

    fun getSet(): List<Locale>? {
        return if (OsUtils.satisfy(OsUtils.T)) {
            val localeMan = appContext.getSystemService<LocaleManager>()
            val locales = localeMan?.applicationLocales
            if (locales == null || locales.isEmpty) null else locales.toList()
        } else {
            LanguageMan(appContext).getLocaleOrNull()?.let { listOf(it) }
        }
    }

    fun set(locale: Locale?) {
        if (OsUtils.satisfy(OsUtils.T)) {
            val list = locale?.let { LocaleList(it) } ?: LocaleList.getEmptyLocaleList()
            val localeMan = appContext.getSystemService<LocaleManager>()
            localeMan?.applicationLocales = list
        } else {
            val lang = locale?.toString() ?: LanguageMan.AUTO
            LanguageMan(appContext).setLanguage(lang)
        }
    }

    fun getRuntime(): List<Locale> {
        val appList = getApp()
        val setList = getSet() ?: return appList
        if (setList.size == 1) return setList + appList.filterNot { it == setList[0] }
        val setListSet = setList.mapTo(HashSet(setList.size)) { it.toRegionalTag() }
        val otherList = appList.filterNot { it.toRegionalTag() in setListSet }
        return setList + otherList
    }

    fun getRuntimeFirst(): Locale {
        return getSet()?.first() ?: getApp()[0]
    }

    fun getBaseContext(context: Context): Context {
        if (OsUtils.satisfy(OsUtils.T)) return context
        val locale = LanguageMan(context).getLocaleOrNull() ?: return context
        return SystemUtil.getLocaleContext(context, locale)
    }
}

// get lang-country only: zh-Hant-TW -> zh-TW
fun Locale.toRegionalTag() = "$language-$country"
