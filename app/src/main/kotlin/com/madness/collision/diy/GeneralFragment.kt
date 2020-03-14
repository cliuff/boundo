package com.madness.collision.diy

import android.os.Bundle
import androidx.fragment.app.Fragment

/**
 * 实现状态栏透明
 * 使用方法：继承这个类并实现 {@link #consumeInsets(Rect) consumeInsets} 方法
 */
internal abstract class GeneralFragment : Fragment() {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val activity = activity
        if (activity !is GeneralActivity) return
        consumeInsets(activity.windowInsets)
    }

    /**
     * 响应窗口 insets
     */
    protected abstract fun consumeInsets(insets: WindowInsets)
}
