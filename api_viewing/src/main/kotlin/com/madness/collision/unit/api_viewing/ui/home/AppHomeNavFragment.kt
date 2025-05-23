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

import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.flowWithLifecycle
import com.madness.collision.main.MainAppHome
import com.madness.collision.unit.api_viewing.ui.list.AppListFragment
import com.madness.collision.unit.api_viewing.ui.org.AppOrgFragment
import com.madness.collision.unit.api_viewing.ui.upd.AppUpdatesFragment
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlin.reflect.KClass

interface AppHomeNavPage {
    var mainAppHome: MainAppHome?
    /** Padding for this page's content. */
    var navContentPadding: PaddingValues
    /** Status bar icons color of this page. */
    val statusBarDarkIcon: StateFlow<Boolean>
    /**
     * Update status bar icons color for this page.
     * Setting the same values will be ignored. */
    fun setStatusBarDarkIcon(isDark: Boolean)

    companion object {
        const val ARG_CONTENT_PADDING: String = "NavPageContentPadding"
    }
}

class AppHomeNavPageImpl : AppHomeNavPage {
    override var mainAppHome: MainAppHome? = null
    override var navContentPadding: PaddingValues by mutableStateOf(PaddingValues())
    private val mutStatusBarDarkIcon = MutableStateFlow(false)
    override val statusBarDarkIcon: StateFlow<Boolean> = mutStatusBarDarkIcon.asStateFlow()

    override fun setStatusBarDarkIcon(isDark: Boolean) {
        mutStatusBarDarkIcon.update { isDark }
    }
}

private val HomeNavContainerId: Int = View.generateViewId()

/** Navigate between fragments. */
class AppHomeNavFragment : Fragment(), AppHomeNav {
    companion object {
        const val ARG_NAV_PAGE = "AppHomeNavPage"
        /** The [arguments][Bundle] that will be passed to the initial page. */
        const val ARG_NAV_ARGUMENTS: String = "AppHomeNavArgs"
        const val ARG_CONTENT_PADDING: String = AppHomeNavPage.ARG_CONTENT_PADDING
    }
    private val navFgmClasses: Array<KClass<out Fragment>> =
        arrayOf(AppUpdatesFragment::class, AppListFragment::class, AppOrgFragment::class)
    private val navFgmTags = navFgmClasses.map { klass -> "AppHome_" + klass.simpleName }
    private var lastContentPadding: PaddingValues? = null
    private val mutStatusBarDarkIcon = MutableStateFlow(false)
    override val statusBarDarkIcon: StateFlow<Boolean> = mutStatusBarDarkIcon.asStateFlow()
    private val statusBarDarkIconJobs: Array<Job?> = arrayOfNulls(navFgmClasses.size)

    fun setContentPadding(paddingValues: PaddingValues) {
        lastContentPadding = paddingValues
        childFragmentManager.fragments.filterIsInstance<AppHomeNavPage>()
            .forEach { page -> page.navContentPadding = paddingValues }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { args ->
            // set initial content padding for the first page to use
            BundleCompat.getParcelable(args, ARG_CONTENT_PADDING, RectF::class.java)
                ?.run { lastContentPadding = PaddingValues.Absolute(left.dp, top.dp, right.dp, bottom.dp) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // container's id must be constant across configuration changes to restore fragments
        return FragmentContainerView(inflater.context).apply { id = HomeNavContainerId }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            // init restored nav pages
            childFragmentManager.fragments.filterIsInstance<AppHomeNavPage>()
                .forEach { page ->
                    // find MainAppHome from host fragment or activity
                    page.mainAppHome = (parentFragment as? MainAppHome) ?: (activity as? MainAppHome)
                    lastContentPadding?.let { page.navContentPadding = it }
                    // collect from the active page only to avoid interference from hidden pages
                    if ((page as Fragment).isHidden.not()) collectStatusBarDarkIcon(page)
                }
        }
        if (savedInstanceState == null) {
            val indexArg = arguments?.getInt(ARG_NAV_PAGE) ?: 0
            val initArgs = arguments?.getBundle(ARG_NAV_ARGUMENTS)
            setNavPage(indexArg.coerceIn(navFgmClasses.indices), initArgs)
        }
    }

    override fun setNavPage(index: Int) =
        setNavPage(index, null)

    private fun setNavPage(index: Int, arguments: Bundle?) {
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
                val direction = when {
                    view?.isLayoutDirectionResolved != true -> LayoutDirection.Ltr
                    view?.layoutDirection == View.LAYOUT_DIRECTION_RTL -> LayoutDirection.Rtl
                    else -> LayoutDirection.Ltr
                }
                val paddingRect = (lastContentPadding ?: PaddingValues()).toRectF(direction)
                // pass content padding as arg to newly created nav pages,
                // only the initial page definitely needs this arg to avoid visual flicker.
                var args = bundleOf(AppHomeNavPage.ARG_CONTENT_PADDING to paddingRect)
                // pass additional custom arguments for individual nav pages
                args = Bundle(arguments ?: Bundle.EMPTY).apply { putAll(args) }
                add(HomeNavContainerId, navFgmClasses[index].java, args, navFgmTags[index])
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
                collectStatusBarDarkIcon(fgm, index)
            }
        }
    }

    private fun collectStatusBarDarkIcon(page: AppHomeNavPage, index: Int = -1) {
        val i = when (index) {
            in statusBarDarkIconJobs.indices -> index
            else -> navFgmClasses.indexOf((page as Fragment)::class)
        }
        if (i !in statusBarDarkIconJobs.indices) {
            IllegalStateException("Nav page index [$i] is not in range [${statusBarDarkIconJobs.indices}].")
                .printStackTrace()
            return
        }
        // Cancel the old job and collect anew,
        // this will update flow with new page's value when switching to a different page.
        statusBarDarkIconJobs[i]?.cancel()
        val lifecycle = viewLifecycleOwner.lifecycle
        page.statusBarDarkIcon
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach { darkIcon -> mutStatusBarDarkIcon.update { darkIcon } }
            .launchIn(lifecycle.coroutineScope)
            .also { statusBarDarkIconJobs[i] = it }
    }

    override fun navBack() {
        setNavPage(-1)
    }
}
