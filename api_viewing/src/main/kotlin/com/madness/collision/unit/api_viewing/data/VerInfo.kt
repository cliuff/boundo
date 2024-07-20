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

package com.madness.collision.unit.api_viewing.data

import android.content.Context
import com.madness.collision.unit.api_viewing.Utils
import com.madness.collision.util.X
import com.madness.collision.util.adapted
import com.madness.collision.util.regexOf
import com.madness.collision.util.toAdapted

internal class VerInfo(val api: Int, sdk: String, val letter: Char) {

    companion object {
        fun targetDisplay(app: ApiViewingApp) = VerInfo(app.targetAPI, isExact = false)

        fun minDisplay(app: ApiViewingApp) = VerInfo(app.minAPI, isExact = false)
    }

    private var codeName: String? = null
    val sdk: String by lazy(LazyThreadSafetyMode.NONE) {
        val patterns = listOf(
            regexOf("""\d+"""),  // integer
            regexOf("""\d+\.(\d+)"""),  // decimal
            regexOf("""(\d+(?:\.\d+)+)([^\d.]+)(\d+(?:\.\d+)+)"""),  // 4.4 - 4.4.4
            regexOf("""(\d+\.\d+)([^\d.\s]+)"""),  // 4.4W
            regexOf("""((?:\d+\.)+)([^\d.\s]+)"""),  // 4.3.x
        )
        try {
            when {
                sdk.matches(patterns[0].toRegex()) -> sdk.toInt().adapted
                sdk.matches(patterns[1].toRegex()) -> {
                    patterns[1].toRegex().find(sdk)?.let {
                        val fixedDigits = it.groupValues[1].length
                        sdk.toFloat().toAdapted(fixedDigits = fixedDigits)
                    } ?: sdk.toFloat().adapted
                }
                sdk.matches(patterns[2].toRegex()) -> {
                    """\d+""".toRegex().replace(sdk) { it.value.toInt().adapted }
                }
                sdk.matches(patterns[3].toRegex()) -> sdk
                sdk.matches(patterns[4].toRegex()) -> sdk
                else -> sdk
            }
        } catch (ignored: Exception) {
            sdk
        }
    }
    val apiText: String by lazy(LazyThreadSafetyMode.NONE) { api.adapted }

    val displaySdk: String
        get() = if (api == X.DEV) "X" else sdk

    val letterOrDev: Char
        get() = if (api == X.DEV) Utils.getDevCodenameLetter() ?: letter else letter

    constructor(api: Int, isExact: Boolean = false, isCompact: Boolean = false)
            : this(api, Utils.getAndroidVersionByAPI(api, isExact, isCompact), Utils.getAndroidLetterByAPI(api))

    fun codeName(context: Context): String {
        if (codeName == null) {
            val codeName = Utils.getAndroidCodenameByAPI(context, api)
            this.codeName = if (codeName.matches("""\d+""".toRegex())) codeName.toInt().adapted else codeName
        }
        return codeName ?: ""
    }
}

internal val VerInfo.verNameOrNull
    get() = sdk.takeUnless { it.isBlank() }

internal fun VerInfo.codenameOrNull(context: Context): String? {
    return codeName(context).takeUnless { it == sdk || it.isBlank() }
}
