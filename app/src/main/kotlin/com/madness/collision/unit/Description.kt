/*
 * Copyright 2020 Clifford Liu
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

package com.madness.collision.unit

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.madness.collision.BuildConfig
import com.madness.collision.util.NameCached

open class Description(val unitName: String, displayNameResId: Int, val iconResId: Int)
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
        return if (mIcon == null) ContextCompat.getDrawable(context, iconResId) else mIcon
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
