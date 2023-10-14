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
import android.os.Parcel
import androidx.lifecycle.*
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import kotlinx.coroutines.*
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicReference


/**
 * [AppDao] proxy to convert query result to [ApiViewingApp] proxy
 * and initialize ignored properties
 */
object DataMaintainer {
    class InterceptException(message: String?) : RuntimeException(message)

    abstract class Interceptor : InvocationHandler, Cloneable {
        abstract fun getDao(): AppDao?

        public override fun clone(): Any {
            return super.clone()
        }
    }

    private fun ApiViewingApp.copyTo(other: ApiViewingApp) {
        val parcel = Parcel.obtain()
        writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        other.readParcel(parcel)
    }

    private suspend fun mapToApp(context: Context, list: List<*>, anApp: ApiViewingApp)
    : List<ApiViewingApp> = coroutineScope {
        val lastIndex = list.lastIndex
        list.mapIndexedNotNull { index, item ->
            if (item !is ApiViewingApp) return@mapIndexedNotNull null
            async(Dispatchers.Default) {
                val app = if (index == lastIndex) anApp else anApp.clone() as ApiViewingApp
                item.copyTo(app)
                // get application info one at a time, may be optimized
                app.initIgnored(context)
                app
            }
        }.map { it.await() }
    }

    fun getDefault(context: Context): AppDao {
        return AppRoom.getDatabase(context).appDao()
    }

    fun get(context: Context, lifecycleOwner: LifecycleOwner, dao: AppDao? = null): AppDao {
        val scopeWrapper = AtomicReference(lifecycleOwner.lifecycleScope)
        val daoWrapper = AtomicReference(dao ?: getDefault(context))
        AppMaintainer.registerCleaner(lifecycleOwner) {
            scopeWrapper.set(null)
            daoWrapper.set(null)
        }
        return get(context, lifecycleOwner, { daoWrapper.get() })
    }

    /**
     * Check [AppDao] method invocations
     */
    private fun get(context: Context, lifecycleOwner: LifecycleOwner, daoGetter: () -> AppDao?): AppDao {
        val interceptor = object : Interceptor() {
            override fun getDao(): AppDao? = daoGetter()

            override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any? {
                method ?: throw InterceptException("method is null")
                val dao = getDao() ?: throw InterceptException("DAO is null")
                val result = try {
                    if (args == null) method.invoke(dao) else method.invoke(dao, *args)
                } catch (e: Exception) {
                    e.printStackTrace()
                    throw InterceptException("invocation failure")
                }
                return when (result) {
                    is ApiViewingApp -> {
                        AppMaintainer.get(context, lifecycleOwner, dao).apply {
                            result.copyTo(this)
                            initIgnored(context)
                        }
                    }
                    is List<*> -> {
                        // assume the whole list is ApiViewingApp if the first one is,
                        // otherwise assume none of it is
                        if (result.isNotEmpty() && result[0] !is ApiViewingApp) return result
                        val anApp = AppMaintainer.get(context, lifecycleOwner, dao)
                        runBlocking { mapToApp(context, result, anApp) }
                    }
                    else -> result
                }
            }
        }
        val clazz = AppDao::class.java
        val proxy = Proxy.newProxyInstance(clazz.classLoader, arrayOf(clazz), interceptor) as AppDao
        return SafeAppDaoProxy(proxy)
    }
}
