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

package com.madness.collision.util

import android.os.LocaleList
import java.text.Collator
import java.util.*
import kotlin.Comparator

object StringUtils {
    private val MIXABLE_LANG = mapOf(
            "en" to listOf("zh")
    )

    /**
     * Impossible to sort in more than the primary language.
     * We can provide special support for languages that have no conflict letters with the primary language.
     * E.g., Chinese or Japanese to English.
     * Note that Japanese would have conflict with Chinese.
     * Suppose dealing with meaningful characters only (excluding punctuations and other symbols),
     * we consider Chinese a superset to English and sort in Chinese directly.
     * So basically we are sorting in chosen second language.
     * Consequently any more language is still ignored.
     */
    fun compareName(localeList: List<Locale>, name1: String, name2: String): Int {
        if (localeList.isEmpty()) return 0
        val primaryLocale = localeList[0]
        val primaryCollator by lazy { Collator.getInstance(primaryLocale) }
        if (localeList.size == 1) return primaryCollator.compare(name1, name2)
        val mixable = MIXABLE_LANG[primaryLocale.language] ?: emptyList()
        val secondaryLocale = localeList[1]
        if (secondaryLocale.language !in mixable) return primaryCollator.compare(name1, name2)
        val secondaryCollator = Collator.getInstance(secondaryLocale)
        return secondaryCollator.compare(name1, name2)
    }

    fun compareName(name1: String, name2: String): Int {
        val locales = if (X.aboveOn(X.N)) {
            val locales = LocaleList.getAdjustedDefault()
            if (locales.isEmpty) listOf(Locale.getDefault()) else {
                val re = ArrayList<Locale>(locales.size())
                for (i in 0 until locales.size()) re.add(i, locales[i])
                re
            }
        } else {
            listOf(Locale.getDefault())
        }
        return compareName(locales, name1, name2)
    }

    /**
     * Used with a list of String
     */
    val comparator: Comparator<in String>
        get() = Comparator(StringUtils::compareName)
}

inline fun <T> Iterable<T>.sortedWithUtilsBy(crossinline selector: (T) -> String): List<T> {
    return this.sortedWith(compareBy(StringUtils.comparator, selector))
}
