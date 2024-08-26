/*
 * Copyright 2024 Clifford Liu
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

package com.madness.collision.unit.api_viewing.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import com.madness.collision.unit.api_viewing.ui.list.AppListFragment
import com.madness.collision.unit.api_viewing.ui.upd.AppUpdatesFragment

/** Navigate between fragments. */
class AppHomeNavFragment : Fragment(), AppHomeNav {
    private val navFgmClasses = arrayOf(AppUpdatesFragment::class, AppListFragment::class)
    private val navFgmTags = navFgmClasses.map { klass -> "AppHome_" + klass.simpleName }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentContainerView(inflater.context).apply { id = View.generateViewId() }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            setNavPage(0)
        }
    }

    override fun setNavPage(index: Int) {
        if (index == -1) {
            val lastFragment = childFragmentManager.fragments.lastOrNull { it.isHidden } ?: return
            val navIndex = navFgmClasses.indexOf(lastFragment::class)
            if (navIndex >= 0) setNavPage(navIndex)
            return
        }
        if (index !in navFgmClasses.indices) {
            IllegalStateException("Nav index [$index] is not in range [${navFgmClasses.indices}].")
                .printStackTrace()
            return
        }

        val fgmManager = childFragmentManager
        val targetFragment = fgmManager.findFragmentByTag(navFgmTags[index])
        val lastVisFragment = fgmManager.fragments.lastOrNull { it.isVisible }
        // already at the target page, abort
        if (targetFragment != null && targetFragment === lastVisFragment) return

        fgmManager.commit {
            // hide other nav pages instead of removing them
            if (lastVisFragment != null) {
                hide(lastVisFragment)
            }
            if (targetFragment != null) {
                show(targetFragment)
            } else {
                add(view?.id ?: 0, navFgmClasses[index].java, null, navFgmTags[index])
            }
            setReorderingAllowed(true)
        }
    }

    override fun navBack() {
        setNavPage(-1)
    }
}
