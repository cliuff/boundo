package com.madness.collision.unit

import android.content.Context
import android.graphics.drawable.Drawable
import com.madness.collision.BuildConfig
import com.madness.collision.util.NameCached

class Description(val unitName: String, displayNameResId: Int, private val iconResId: Int)
    : DynamicItem(displayNameResId,  {
    Unit.getDescription(unitName)?.let {
        UnitDescFragment.newInstance(it)
    } ?: UnitDescFragment()
}) {

    val packagedName: String = "${BuildConfig.BUILD_PACKAGE}.unit.$unitName"
    private var mIcon: Drawable? = null
    private var mCheckers: Array<out Checker>? = null

    val checkers: Array<out Checker>
        get() = mCheckers!!
    val hasChecker: Boolean
        get() = mCheckers != null
    var descRes: Int = 0
        private set

    fun getIcon(context: Context): Drawable? {
        return if (mIcon == null) context.getDrawable(iconResId) else mIcon
    }

    class Checker(override val nameResId: Int, private val checker: (context: Context) -> Boolean): NameCached {

        override var name: String = ""

        constructor(name: String, checker: (context: Context) -> Boolean): this(0, checker) {
            this.name = name
        }

        fun check(context: Context) = checker.invoke(context)
    }

    fun setRequirement(vararg checkers: Checker): Description {
        availabilityChecker = {
            var re = true
            for (c in checkers) {
                if (c.check(it)) continue
                re = false
                break
            }
            re
        }
        mCheckers = checkers
        return this
    }

    fun setDescResId(resId: Int): Description {
        descRes = resId
        return this
    }

}
