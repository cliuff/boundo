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
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference


/**
 * [ApiViewingApp] proxy to update database record
 */
object AppMaintainer {

    fun registerCleaner(lifecycleOwner: LifecycleOwner, block: () -> Unit) {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            val lifecycle = lifecycleOwner.lifecycle
            lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
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
        return get({ daoWrapper.get() }, scopeGetter)
    }

    /**
     * Update record after [ApiViewingApp.retrieveConsuming] invocation.
     * Somehow ByteBuddy proxy cannot be garbage collected, causing context leaks.
     * So, use with a lifecycle owner.
     */
    private fun get(daoGetter: () -> AppDao?, scopeGetter: () -> CoroutineScope?): ApiViewingApp {
        return MaintainedApp(daoGetter, scopeGetter)
    }
}
