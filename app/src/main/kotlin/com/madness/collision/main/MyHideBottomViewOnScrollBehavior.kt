package com.madness.collision.main

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import java.lang.ref.WeakReference

class MyHideBottomViewOnScrollBehavior<V : View> : HideBottomViewOnScrollBehavior<V> {

    private var syncBehavior: HideBottomViewOnScrollBehavior<View>? = null
    private var syncViewRef: WeakReference<View>? = null
    private var isSyncTheFirstTimeExcluded: Boolean = false
    var onSlidedUpCallback: (() -> Unit)? = null
    var onSlidedDownCallback: (() -> Unit)? = null

    constructor(): super()

    constructor(context: Context, attrs: AttributeSet): super(context, attrs)

    override fun slideUp(child: V) {
        super.slideUp(child)
        onSlidedUpCallback?.invoke()
        if (isSyncTheFirstTimeExcluded)
            syncViewRef?.get()?.let { syncBehavior?.slideUp(it) }
        else isSyncTheFirstTimeExcluded = true
    }

    override fun slideDown(child: V) {
        super.slideDown(child)
        onSlidedDownCallback?.invoke()
        if (isSyncTheFirstTimeExcluded)
            syncViewRef?.get()?.let { syncBehavior?.slideDown(it) }
        else isSyncTheFirstTimeExcluded = true
    }

    fun setupSync(view: View, shouldExcludeTheFirstTime: Boolean = true) {
        val params = view.layoutParams as CoordinatorLayout.LayoutParams
        syncBehavior = params.behavior as HideBottomViewOnScrollBehavior
        isSyncTheFirstTimeExcluded = !shouldExcludeTheFirstTime
        syncViewRef = WeakReference(view)
    }

}
