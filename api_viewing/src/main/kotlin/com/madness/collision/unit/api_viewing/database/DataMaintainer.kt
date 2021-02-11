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
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import kotlinx.coroutines.*
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy


/**
 * [AppDao] proxy to convert query result to [ApiViewingApp] proxy
 * and initialize ignored properties
 */
object DataMaintainer {

    abstract class Interceptor : InvocationHandler, Cloneable {
        lateinit var target: AppDao

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

    private suspend fun mapToApp(list: List<*>, anApp: ApiViewingApp)
    : List<ApiViewingApp> = coroutineScope {
        val lastIndex = list.lastIndex
        list.mapIndexedNotNull { index, item ->
            if (item !is ApiViewingApp) return@mapIndexedNotNull null
            async(Dispatchers.Default) {
                val app = if (index == lastIndex) anApp else anApp.clone() as ApiViewingApp
                item.copyTo(app)
                app.initIgnored()
                app
            }
        }.map { it.await() }
    }

    /**
     * Check [AppDao] method invocations
     */
    /**
     * Check [AppDao] method invocations
     */
    fun get(context: Context, scope: CoroutineScope,
            dao: AppDao = AppRoom.getDatabase(context, scope).appDao()): AppDao {
        val interceptor = object : Interceptor() {
            override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any? {
                method ?: return null
                val result = try {
                    if (args == null) method.invoke(target) else method.invoke(target, *args)
                } catch (e: Exception) {
                    e.printStackTrace()
                    return null
                }
                return when (result) {
                    is ApiViewingApp -> {
                        AppMaintainer.get(context, scope, target).apply {
                            result.copyTo(this)
                            initIgnored()
                        }
                    }
                    is List<*> -> {
                        // assume the whole list is ApiViewingApp if the first one is,
                        // otherwise assume none of it is
                        if (result.isNotEmpty() && result[0] !is ApiViewingApp) return result
                        val anApp = AppMaintainer.get(context, scope, target)
                        runBlocking { mapToApp(result, anApp) }
                    }
                    else -> result
                }
            }
        }
        val clazz = dao.javaClass
        val proxy = Proxy.newProxyInstance(clazz.classLoader, clazz.interfaces, interceptor) as AppDao
        interceptor.target = dao
        return proxy
    }
}
