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

import android.content.Context
import android.graphics.Bitmap
import com.madness.collision.R
import com.madness.collision.util.BlurUtils.blurred
import com.madness.collision.util.os.OsUtils

private val tests: Array<Pair<Boolean, (Context) -> Unit>> = arrayOf(
    false to { context ->
        val bitmap = GraphicsUtil.drawOn(context, R.drawable.app_icon_banner, 320, 180)
        var blurred: Bitmap? = null
        if (OsUtils.satisfy(OsUtils.S)) blurred = bitmap.blurred(20f, 20f)
        val blurredLegacy = GaussianBlur(context).blurOnce(bitmap, 20f)
        blurred to blurredLegacy
    },
)

object TestPlayground {
    private var test: ((Context) -> Unit)? = null
    val hasTest: Boolean
        get() = test != null

    init {
        tests.forEach { (active, block) -> test(active, block) }
    }

    fun test(active: Boolean, block: (Context) -> Unit) {
        if (!active) return
        test = block
    }

    fun start(context: Context) {
        test?.invoke(context)
    }
}
