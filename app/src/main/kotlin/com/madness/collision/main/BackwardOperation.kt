package com.madness.collision.main

import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import java.lang.ref.WeakReference
import kotlin.reflect.full.createInstance

class BackwardOperation(forwardFragment: Fragment, backwardFragment: Fragment, val operationFlags: BooleanArray) {

    class Page(fragment: Fragment) {
        private val ref = WeakReference(fragment)
        private val clazz = fragment::class
        private val args = fragment.arguments
        val hasRef: Boolean
            get() = ref.get() != null
        val fragment: Fragment
            get() = ref.get() ?: newFragment
        val refFragment: Fragment?
            get() = ref.get()
        val newFragment: Fragment
            get() = clazz.createInstance().apply {
                arguments = args
            }
    }

    val forwardPage = Page(forwardFragment)
    val backwardPage = Page(backwardFragment)

    var cachedCallback: OnBackPressedCallback? = null
}
