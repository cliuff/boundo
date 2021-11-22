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

import androidx.fragment.app.Fragment
import com.madness.collision.util.MathUtils.boundMin
import kotlin.math.roundToInt

object AppUtils {
    /**
     * Set minimum bottom margin
     */
    fun Fragment.asBottomMargin(margin: Int): Int {
        val index = 0
        var minBottomMargin = mainApplication.minBottomMargin[index]
        if (minBottomMargin < 0) {
            val context = context ?: return margin
            val pref = P.APP_MARGIN_BOTTOM_MIN
            minBottomMargin = X.size(context, pref, X.DP).roundToInt()
            mainApplication.minBottomMargin[index] = minBottomMargin
        }
        val minMargin = minBottomMargin + mainApplication.insetBottom
        return margin.boundMin(minMargin)
    }
}