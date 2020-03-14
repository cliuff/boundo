package com.madness.collision.unit

import kotlin.reflect.KClass

abstract class UnitAccess(protected val unitName: String) {

    protected fun getMethod(name: String, vararg args: KClass<*>) : BridgeMethod {
        return BridgeMethod(unitName, name, *args)
    }

    protected fun invokeWithoutArg(name: String) {
        getMethod(name).invoke()
    }
}
