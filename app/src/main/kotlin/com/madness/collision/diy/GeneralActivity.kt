package com.madness.collision.diy

import android.graphics.Rect
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity


internal class WindowInsets(val left: Int = 0, val top: Int = 0, val right: Int = 0, val bottom: Int = 0){
    constructor(rect: Rect): this(rect.left, rect.top, rect.right, rect.bottom)

    constructor(insets: android.view.WindowInsets): this(
            insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
            insets.systemWindowInsetRight, insets.systemWindowInsetBottom
    )
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
            mWindowInsets = WindowInsets(
                    insets.systemWindowInsetLeft,
                    insets.systemWindowInsetTop,
                    insets.systemWindowInsetRight,
                    insets.systemWindowInsetBottom
            )
            consumeInsets(windowInsets)
            return@setOnApplyWindowInsetsListener insets
        }
    }

    /**
     * 响应窗口 insets
     */
    protected abstract fun consumeInsets(insets: WindowInsets)
}
