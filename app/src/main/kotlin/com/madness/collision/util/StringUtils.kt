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

package com.madness.collision.util

import com.madness.collision.util.config.LocaleUtils
import com.madness.collision.util.ui.appLocale
import java.text.Collator
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.Comparator

object StringUtils {
    private val MIXABLE_LANG = mapOf("en" to listOf("zh"))

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
        return getComparator(localeList).compare(name1, name2)
    }

    /**
     * Caution: this function is only suitable for one-time usage,
     * use comparator or the other overload instead for usages in a loop
     * (avoid making locale list every iteration)
     */
    fun compareName(name1: String, name2: String): Int {
        return compareName(LocaleUtils.getRuntime(), name1, name2)
    }

    fun getComparator(localeList: List<Locale>): Comparator<in String> {
        if (localeList.isEmpty()) return Comparator { _, _ -> 0 }
        val collators = localeList.take(2)
            .map { lazy(LazyThreadSafetyMode.NONE) { Collator.getInstance(it) } }
        if (localeList.size == 1) return collators[0].value
        val primaryLocale = localeList[0]
        val mixable = MIXABLE_LANG[primaryLocale.language] ?: emptyList()
        val secondaryLocale = localeList[1]
        if (secondaryLocale.language !in mixable) return collators[0].value
        return collators[1].value
    }

    /**
     * Used with a list of String
     */
    val comparator: Comparator<in String> get() = getComparator(LocaleUtils.getRuntime())
}

inline fun <T> Iterable<T>.sortedWithUtilsBy(crossinline selector: (T) -> String): List<T> {
    return this.sortedWith(compareBy(StringUtils.comparator, selector))
}

val Int.adapted: String
    get() = String.format(appLocale, "%d", this)

val Float.adapted: String
    get() = toAdapted()

// locale friendly (numbers in Arabic, decimal in Spanish), proper format (0.0 -> 0)
// Fractional digits are a number of digits after the decimal separator ( . )
// Float numbers are inaccurate, 0.001f results in 0.0010000000474974513, 0.3 -> 0.30000001...
fun Float.toAdapted(maxFractionDigits: Int = 5, fixedDigits: Int = -1): String {
    val locale = appLocale
    return if (fixedDigits >= 0) {  // fixed number of fractional digits
        String.format(locale, "%.${fixedDigits}f", this)
    } else {
        DecimalFormat("0", DecimalFormatSymbols.getInstance(locale)).apply {
            maximumFractionDigits = maxFractionDigits
        }.format(this)
    }
}
