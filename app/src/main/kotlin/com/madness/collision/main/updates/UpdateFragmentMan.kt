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
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.madness.collision.unit.Description
import com.madness.collision.unit.Unit

interface UpdateFragmentHost {
    val context: Context
    val fragmentManager: FragmentManager
    val updateContainerView: ViewGroup
    fun createUpdateHeader(desc: Description, parent: ViewGroup): View
}

class UpdateFragmentMan(host: UpdateFragmentHost) : UpdateFragmentHost by host {
    fun restoreUpdateFragment(updateFragment: Pair<String, Fragment>) {
        val (unitName: String, fragment: Fragment) = updateFragment
        val container = addUpdateFragmentContainer(unitName, -1) ?: return
        val fMan = fragmentManager
        if (fragment.isAdded) {
            // avoid IllegalStateException: Can't change container ID of fragment
            fMan.beginTransaction().remove(fragment).commitNowAllowingStateLoss()
            fMan.executePendingTransactions()
        }
        fMan.beginTransaction().replace(container.id, fragment).commitNowAllowingStateLoss()
    }

    fun addUpdateFragment(updateFragment: Pair<String, Fragment>, index: Int = -1) {
        val (unitName: String, fragment: Fragment) = updateFragment
        if (fragment.isAdded) return
        val container = addUpdateFragmentContainer(unitName, index) ?: return
        fragmentManager.beginTransaction().add(container.id, fragment).commitNowAllowingStateLoss()
    }

    private fun addUpdateFragmentContainer(unitName: String, index: Int) : ViewGroup? {
        val container = getUpdateFragmentContainer(unitName) ?: return null
        updateContainerView.run { if (index < 0) addView(container) else addView(container, index) }
        return container
    }

    private fun getUpdateFragmentContainer(unitName: String) : ViewGroup? {
        val description = Unit.getDescription(unitName) ?: return null
        val container = LinearLayout(context).apply {
            id = View.generateViewId()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
        }
        container.addView(createUpdateHeader(description, container))
        return container
    }

    fun removeUpdateFragment(updateFragment: Pair<String, Fragment>, index: Int) {
        if (index > updateContainerView.size - 1) return
        val (_, fragment: Fragment) = updateFragment
        fragmentManager.beginTransaction().remove(fragment).commitNowAllowingStateLoss()
        updateContainerView.removeViewAt(index)
    }

}