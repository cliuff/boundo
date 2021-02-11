/*
 * Copyright 2021 Clifford Liu
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

package com.madness.collision.unit.api_viewing.database

import android.content.Context
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.bytebuddy.ByteBuddy
import net.bytebuddy.android.AndroidClassLoadingStrategy
import net.bytebuddy.dynamic.scaffold.TypeValidation
import net.bytebuddy.implementation.MethodDelegation
import net.bytebuddy.implementation.bind.annotation.RuntimeType
import net.bytebuddy.implementation.bind.annotation.SuperCall
import net.bytebuddy.implementation.bind.annotation.This
import net.bytebuddy.matcher.ElementMatchers
import java.util.concurrent.Callable


/**
 * [ApiViewingApp] proxy to update database record
 */
object AppMaintainer {

    open class BaseInterceptor : Cloneable {
        public override fun clone(): Any {
            return super.clone()
        }
    }

    abstract class Interceptor : BaseInterceptor() {
        @RuntimeType
        abstract fun intercept(@This proxy: Any, @SuperCall superCall: Callable<*>): Any?
    }

    /**
     * Update record after [ApiViewingApp.retrieveNativeLibraries] invocation
     */
    fun get(context: Context, scope: CoroutineScope, dao: AppDao = DataMaintainer.get(context, scope))
    : ApiViewingApp {
        val interceptor = object : Interceptor() {
            @RuntimeType
            override fun intercept(@This proxy: Any, @SuperCall superCall: Callable<*>): Any? {
                return try {
                    superCall.call().also {
                        if (proxy !is ApiViewingApp) return@also
                        // update asynchronously
                        scope.launch(Dispatchers.Default) {
                            dao.insert(proxy)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        }
        val strategy = AndroidClassLoadingStrategy.Wrapping(
                context.getDir("generated", Context.MODE_PRIVATE))
        val dynamicType = ByteBuddy().with(TypeValidation.DISABLED)
                .subclass(ApiViewingApp::class.java)
                .method(ElementMatchers.named(ApiViewingApp::retrieveNativeLibraries.name))
                .intercept(MethodDelegation.to(interceptor))
                .make()
                .load(javaClass.classLoader, strategy)
                .loaded
        return dynamicType.newInstance() as ApiViewingApp
    }
}
