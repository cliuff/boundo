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
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.madness.collision.util.os.OsUtils


internal class WindowInsets {
    private var mLeft: Int = 0
    val left: Int
        get() = mLeft
    private var mTop: Int = 0
    val top: Int
        get() = mTop
    private var mRight: Int = 0
    val right: Int
        get() = mRight
    private var mBottom: Int = 0
    val bottom: Int
        get() = mBottom

    private fun init(left: Int = 0, top: Int = 0, right: Int = 0, bottom: Int = 0) {
        mLeft = left
        mTop = top
        mRight = right
        mBottom = bottom
    }

    constructor(left: Int = 0, top: Int = 0, right: Int = 0, bottom: Int = 0) {
        init(left, top, right, bottom)
    }

    constructor(rect: Rect) : this(rect.left, rect.top, rect.right, rect.bottom)

    constructor(insets: android.view.WindowInsets) {
        if (OsUtils.satisfy(OsUtils.R)) {
            insets.getInsets(android.view.WindowInsets.Type.systemBars()).run {
                init(left, top, right, bottom)
            }
        } else {
            getInsetsLegacy(insets).run {
                init(left, top, right, bottom)
            }
        }
    }

    @Suppress("deprecation")
    private fun getInsetsLegacy(insets: android.view.WindowInsets): Rect {
        return Rect(insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
    }
}


/**
 * 实现状态栏透明
 * 使用方法：继承这个类并实现 {@link #consumeInsets(Rect) consumeInsets} 方法
 */
internal abstract class GeneralActivity : AppCompatActivity() {
    private var mWindowInsets: WindowInsets = WindowInsets()
    val windowInsets: WindowInsets
        get() = mWindowInsets

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        SystemUtil.applyEdge2Edge(window)
        applyInsets()
    }

    /**
     * 获得状态栏高度、导航栏高度等
     */
    private fun applyInsets() {
        val root = window.decorView.rootView
        root!!.setOnApplyWindowInsetsListener { _, insets ->
            mWindowInsets = WindowInsets(insets)
            consumeInsets(windowInsets)
            return@setOnApplyWindowInsetsListener insets
        }
    }

    /**
     * 响应窗口 insets
     */
    protected abstract fun consumeInsets(insets: WindowInsets)
}
