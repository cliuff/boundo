package com.madness.collision.unit

import android.content.Context
import androidx.fragment.app.Fragment
import com.madness.collision.util.NameCached

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
