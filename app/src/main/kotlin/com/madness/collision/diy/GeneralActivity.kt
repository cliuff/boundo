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

package com.madness.collision.diy

import android.graphics.Rect
import com.madness.collision.util.os.OsUtils


class WindowInsets {
    private var _start: Int = 0
    // rtl layout friendly
    val start: Int
        get() = _start
    private var _top: Int = 0
    val top: Int
        get() = _top
    private var _end: Int = 0
    val end: Int
        get() = _end
    private var _bottom: Int = 0
    val bottom: Int
        get() = _bottom

    private fun init(left: Int = 0, top: Int = 0, right: Int = 0, bottom: Int = 0) {
        _start = left
        _top = top
        _end = right
        _bottom = bottom
    }

    constructor(left: Int = 0, top: Int = 0, right: Int = 0, bottom: Int = 0) {
        init(left, top, right, bottom)
    }

    constructor(rect: Rect) : this(rect.left, rect.top, rect.right, rect.bottom)

    constructor(insets: android.view.WindowInsets, isRtl: Boolean) {
        if (OsUtils.satisfy(OsUtils.R)) {
            insets.getInsets(android.view.WindowInsets.Type.systemBars()).run {
                if (isRtl) init(right, top, left, bottom) else init(left, top, right, bottom)
            }
        } else {
            getInsetsLegacy(insets).run {
                if (isRtl) init(right, top, left, bottom) else init(left, top, right, bottom)
            }
        }
    }

    @Suppress("deprecation")
    private fun getInsetsLegacy(insets: android.view.WindowInsets): Rect {
        return Rect(insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
    }
}
