/*
 * Copyright 2023 Clifford Liu
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

package com.madness.collision.main.updates

import android.content.Context
import androidx.fragment.app.Fragment
import com.madness.collision.unit.DescRetriever
import com.madness.collision.unit.StatefulDescription
import com.madness.collision.unit.Unit
import com.madness.collision.unit.UpdatesProvider

class UnitUpdateInfo(val unitName: String, val fragment: Fragment, val hasNewUpdate: Boolean)

/** UpdateProviderManager */
class UpdateProviderMan {
    // used for assignment and thread-safe modification
    private var _updatesProviders: MutableList<Pair<String, UpdatesProvider>> = mutableListOf()
    private val updatesProviders: List<Pair<String, UpdatesProvider>> by ::_updatesProviders

    fun init(context: Context) {
        val descList = DescRetriever(context).includePinState().doFilter().retrieveInstalled()
        _updatesProviders = descList.mapNotNullTo(ArrayList(descList.size)) {
            Unit.getUpdates(it.unitName)?.run { it.unitName to this }
        }
    }

    fun getUpdateInfo(host: Fragment, oldFMap: Map<String, Fragment>): List<UnitUpdateInfo> {
        return updatesProviders.mapNotNull m@{ (unitName, provider) ->
            val newUpdate = provider.hasNewUpdate(host) ?: return@m null
            // query old list first in case fragments were restored
            val f = oldFMap[unitName] ?: provider.fragment ?: return@m null
            UnitUpdateInfo(unitName, f, newUpdate)
        }
    }

    /**
     * List changes include addition and deletion but no update
     */
    fun updateItem(host: Fragment, stateful: StatefulDescription, add: (Fragment) -> kotlin.Unit, remove: () -> kotlin.Unit) {
        val isAddition = stateful.isPinned
        if (isAddition) {
            if (updatesProviders.any { it.first == stateful.unitName }) return
            val provider = Unit.getUpdates(stateful.unitName) ?: return
            synchronized(_updatesProviders) { _updatesProviders.add(stateful.unitName to provider) }
            if (!provider.hasUpdates(host)) return
            val fragment = provider.fragment ?: return
            add(fragment)
        } else {
            for (i in updatesProviders.indices) {
                val provider = updatesProviders[i]
                if (provider.first != stateful.unitName) continue
                synchronized(_updatesProviders) { _updatesProviders.removeAt(i) }
                break
            }
            remove()
        }
    }
}