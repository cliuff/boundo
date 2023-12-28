package com.madness.collision.util.controller

import android.content.Context
import androidx.fragment.app.Fragment

interface NameCached {
    var name: String
    val nameResId: Int

    fun getName(context: Context): String {
        return if (name.isEmpty()) context.getString(nameResId) else name
    }
}

open class DynamicItem(override val nameResId: Int, private val descriptionPageGetter: (() -> Fragment)? = null): NameCached {

    override var name: String = ""
    protected var availabilityChecker: ((context: Context) -> Boolean)? = null

    val hasDescription: Boolean
        get() = descriptionPageGetter != null

    val descriptionPage: Fragment?
        get() = descriptionPageGetter?.invoke()

    fun isAvailable(context: Context): Boolean {
        return availabilityChecker?.invoke(context) ?: true
    }

}
