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
import androidx.lifecycle.*
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
import java.util.concurrent.atomic.AtomicReference


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

    fun registerCleaner(lifecycleOwner: LifecycleOwner, block: () -> Unit) {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            val lifecycle = lifecycleOwner.lifecycle
            lifecycle.addObserver(object : LifecycleObserver {
                @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                fun onDestroy() {
                    lifecycle.removeObserver(this)
                    block.invoke()
                }
            })
        }
    }

    fun get(context: Context, lifecycleOwner: LifecycleOwner, appDao: AppDao? = null): ApiViewingApp {
        val scopeWrapper = AtomicReference<CoroutineScope?>(lifecycleOwner.lifecycleScope)
        val scopeGetter = { scopeWrapper.get() }
        val originalDao = DataMaintainer.getDefault(context)
        val dao = appDao ?: DataMaintainer.get(context, lifecycleOwner, originalDao)
        val daoWrapper = AtomicReference(dao)
        registerCleaner(lifecycleOwner) {
            scopeWrapper.set(null)
            daoWrapper.set(null)
        }
        return get(context, { daoWrapper.get() }, scopeGetter)
    }

    /**
     * Update record after [ApiViewingApp.retrieveConsuming] invocation.
     * Somehow ByteBuddy proxy cannot be garbage collected, causing context leaks.
     * So, use with a lifecycle owner.
     */
    private fun get(context: Context, daoGetter: () -> AppDao?, scopeGetter: () -> CoroutineScope?): ApiViewingApp {
        val interceptor = object : Interceptor() {
            @RuntimeType
            override fun intercept(@This proxy: Any, @SuperCall superCall: Callable<*>): Any? {
                return try {
                    superCall.call().also {
                        if (proxy !is ApiViewingApp) return@also
                        val dao = daoGetter.invoke() ?: return@also
                        // update asynchronously
                        scopeGetter.invoke()?.launch(Dispatchers.Default) {
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
                .method(ElementMatchers.named(ApiViewingApp::retrieveConsuming.name))
                .intercept(MethodDelegation.to(interceptor))
                .make()
                .load(javaClass.classLoader, strategy)
                .loaded
        return dynamicType.newInstance() as ApiViewingApp
    }
}
