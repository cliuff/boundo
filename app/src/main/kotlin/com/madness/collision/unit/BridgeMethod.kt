package com.madness.collision.unit

import com.madness.collision.util.objectInstance
import kotlin.reflect.KClass

class BridgeMethod(clazz: Class<Bridge>?, methodName: String, vararg args: KClass<*>) {

    private val bridgeInstance = clazz?.objectInstance
    private val method = clazz?.getDeclaredMethod(methodName, *(args.map { it.java }.toTypedArray()))?.apply { isAccessible = true }

    constructor(unitName: String, methodName: String, vararg args: KClass<*>): this(Unit.getBridgeClass(unitName), methodName, *args)

    fun invoke(vararg args: Any?): Any? {
        return method?.invoke(bridgeInstance, *args)
    }
}
