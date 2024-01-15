package com.madness.collision.unit

import androidx.fragment.app.Fragment
import kotlin.reflect.KClass

abstract class Bridge {
    abstract val unitName: String
    /**
     * Arguments
     */
    open val args: List<KClass<*>> = emptyList()

    abstract fun getUnitInstance(vararg args: Any?): Unit

    open fun getUpdates(): UpdatesProvider? = null
    open fun getSettings(): Fragment? = null
    open fun getAccessor(): Any? = null
}
