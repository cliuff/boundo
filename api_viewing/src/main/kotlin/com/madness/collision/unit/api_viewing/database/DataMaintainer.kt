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
import com.madness.collision.unit.api_viewing.apps.DumbAppRepo
import com.madness.collision.unit.api_viewing.apps.toRecApps
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import kotlinx.coroutines.runBlocking
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

internal class InterceptException(message: String?) : RuntimeException(message)

private abstract class Interceptor : InvocationHandler, Cloneable {
    abstract fun getDao(): AppDao?
    public override fun clone(): Any = super.clone()
}

internal fun AppDaoProxy(context: Context) =
    AppDaoProxy(context, AppRoom.getDatabase(context).appDao())

/**
 * [AppDao] proxy to convert query result to [ApiViewingApp] proxy
 * and initialize ignored properties.
 */
internal fun AppDaoProxy(context: Context, baseDao: AppDao): AppDao {
    val interceptor = object : Interceptor() {
        override fun getDao(): AppDao? = baseDao

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
                    DumbAppRepo(dao).getMaintainedApp().apply {
                        assignPersistentFieldsFrom(result, deepCopy = false)
                        initIgnored(context)
                    }
                }
                is List<*> -> {
                    // assume the whole list is ApiViewingApp if the first one is,
                    // otherwise assume none of it is
                    if (result.isNotEmpty() && result[0] !is ApiViewingApp) return result
                    val anApp = DumbAppRepo(dao).getMaintainedApp()
                    runBlocking { result.toRecApps(context, anApp) }
                }
                else -> result
            }
        }
    }
    val clazz = AppDao::class.java
    val proxy = Proxy.newProxyInstance(clazz.classLoader, arrayOf(clazz), interceptor) as AppDao
    return SafeAppDaoProxy(proxy)
}
