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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import com.madness.collision.main.MainAppHome
import com.madness.collision.unit.api_viewing.ui.list.AppListFragment
import com.madness.collision.unit.api_viewing.ui.upd.AppUpdatesFragment
import kotlin.reflect.KClass

interface AppHomeNavPage {
    var mainAppHome: MainAppHome?
    /** Padding for this page's content. */
    var navContentPadding: PaddingValues
}

class AppHomeNavPageImpl : AppHomeNavPage {
    override var mainAppHome: MainAppHome? = null
    override var navContentPadding: PaddingValues by mutableStateOf(PaddingValues())
}

private val HomeNavContainerId: Int = View.generateViewId()

/** Navigate between fragments. */
class AppHomeNavFragment : Fragment(), AppHomeNav {
    private val navFgmClasses: Array<KClass<out Fragment>> =
        arrayOf(AppUpdatesFragment::class, AppListFragment::class)
    private val navFgmTags = navFgmClasses.map { klass -> "AppHome_" + klass.simpleName }
    private var lastContentPadding: PaddingValues? = null

    fun setContentPadding(paddingValues: PaddingValues) {
        lastContentPadding = paddingValues
        childFragmentManager.fragments.filterIsInstance<AppHomeNavPage>()
            .forEach { page -> page.navContentPadding = paddingValues }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // container's id must be constant across configuration changes to restore fragments
        return FragmentContainerView(inflater.context).apply { id = HomeNavContainerId }
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
                if (targetFragment.run { isHidden && view != null }) {
                    show(targetFragment)
                } else if (targetFragment.isAdded) {
                    remove(targetFragment)
                    add(HomeNavContainerId, targetFragment, navFgmTags[index])
                } else {
                    Log.d("AppHomeNavFragment", "Target fragment is in a weird state.")
                }
            } else {
                add(HomeNavContainerId, navFgmClasses[index].java, null, navFgmTags[index])
            }
            setReorderingAllowed(true)
        }

        // enqueue action (after async commit) to set content padding of restored/newly added page
        view?.post {
            val fgm = fgmManager.findFragmentByTag(navFgmTags[index])
            if (fgm is AppHomeNavPage) {
                // find MainAppHome from host fragment or activity
                fgm.mainAppHome = (parentFragment as? MainAppHome) ?: (activity as? MainAppHome)
                lastContentPadding?.let { fgm.navContentPadding = it }
            }
        }
    }

    override fun navBack() {
        setNavPage(-1)
    }
}
