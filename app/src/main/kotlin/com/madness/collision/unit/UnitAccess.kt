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

import com.madness.collision.util.objectInstance
import kotlin.reflect.KClass

abstract class UnitAccess(protected val unitName: String) {

    protected fun getMethod(name: String, vararg args: KClass<*>) : BridgeMethod {
        return BridgeMethod(unitName, name, *args)
    }

    protected fun invokeWithoutArg(name: String): Any? {
        return getMethod(name).invoke()
    }
}

class BridgeMethod(clazz: Class<Bridge>?, methodName: String, vararg args: KClass<*>) {
    private val bridgeInstance = clazz?.objectInstance
    private val method = clazz?.getDeclaredMethod(methodName, *(args.map { it.java }.toTypedArray()))?.apply { isAccessible = true }

    constructor(unitName: String, methodName: String, vararg args: KClass<*>): this(Unit.getBridgeClass(unitName), methodName, *args)

    fun invoke(vararg args: Any?): Any? {
        return method?.invoke(bridgeInstance, *args)
    }
}
