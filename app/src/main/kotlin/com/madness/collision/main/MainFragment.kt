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

package com.madness.collision.main

import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updatePaddingRelative
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.MutableLiveData
import com.google.android.material.navigation.NavigationBarView
import com.madness.collision.Democratic
import com.madness.collision.R
import com.madness.collision.databinding.FragmentMainBinding
import com.madness.collision.main.more.MoreFragment
import com.madness.collision.main.updates.UpdatesFragment
import com.madness.collision.util.*
import com.madness.collision.util.controller.getSavedFragment
import com.madness.collision.util.controller.saveFragment
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.math.roundToInt

class MainFragment : TaggedFragment(), Democratic {
    override val category: String = "Main"
    override val id: String = "Main"

    companion object {
        private const val STATE_KEY_NAV_UP = "NavUp"
        private const val STATE_KEY_NAV_ITEM = "NavItem"
    }

    // session data
    private var isNavUp = false
    private val viewModel: MainPageViewModel by viewModels()
    private val mainViewModel: MainViewModel by activityViewModels()
    // ui appearance data
    private var primaryNavBarConfig: SystemBarConfig? = null
    private var isLand: Boolean? = null
    private var bottomNavHeight: Int = 0
    // views
    private lateinit var viewBinding: FragmentMainBinding
    private lateinit var navFragments: Array<Lazy<Fragment>>
    // android
    private val animator = MainAnimator()

    // this getter returns null before isLand is initialized, to avoid inconsistent value
    private val background: View?
        get() = isLand?.let { if (it) viewBinding.mainLinear!! else viewBinding.mainFrame }

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        toolbar.visibility = View.INVISIBLE
        (toolbar.tag as View?)?.isVisible = false  // hide toolbar divider
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val updatesMode = arguments?.getInt(UpdatesFragment.ARG_MODE)
        val f = (0..2).map f@{ i: Int ->
            savedInstanceState ?: return@f null
            childFragmentManager.getSavedFragment<Fragment>(savedInstanceState, STATE_KEY_NAV_ITEM + i)
        }
        navFragments = arrayOf(
            lazy {
                f[0] ?: run {
                    updatesMode ?: return@run UpdatesFragment()
                    UpdatesFragment.newInstance(updatesMode)
                }
            },
            lazy { f[1] ?: MainUnitsFragment() },
            lazy { f[2] ?: MoreFragment() },
        )
        val savedIndexes = f.mapIndexedNotNull { i, _ -> i }
        // manually initialize lazy for saved ones,
        // so their initialization states are checked correctly in onSaveInstanceState
        savedIndexes.forEach { navFragments[it].value }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = FragmentMainBinding.inflate(inflater)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = context ?: return
        setupViewModel(context)
        // invocation will clear stack
        setupUi(context)
        // restore session
        if (savedInstanceState?.getBoolean(STATE_KEY_NAV_UP) == false) hideNav()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        navFragments.forEachIndexed { i, lazy ->
            if (lazy.isInitialized().not()) return@forEachIndexed  // continue
            childFragmentManager.saveFragment(outState, STATE_KEY_NAV_ITEM + i, lazy.value)
        }
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_KEY_NAV_UP, isNavUp)
    }

    private fun setupViewModel(context: Context) {
        democratize(mainViewModel)
        mainViewModel.insetTop.observe(viewLifecycleOwner) {
            viewBinding.mainTB.run {
                updatePaddingRelative(top = it)
                measure()
                mainViewModel.contentWidthTop.value = measuredHeight
            }
            // override navigation rail's automatic insets handling, todo remove after material 1.5
            lifecycleScope.launch(Dispatchers.Default) {
                delay(50)
                withContext(Dispatchers.Main) {
                    viewBinding.mainSideNav?.updatePaddingRelative(top = it)
                }
            }
        }
        mainViewModel.insetBottom.observe(viewLifecycleOwner) {
            bottomNavHeight = viewBinding.mainBottomNav?.run {
                updatePaddingRelative(bottom = it)
                measure()
                measuredHeight
            } ?: it
            viewModel.bottomContentWidth.value = bottomNavHeight
            viewBinding.mainShowBottomNav?.let { showingBtn ->
                val margin = it + X.size(context, 20f, X.DP).roundToInt()
                showingBtn.alterMargin(bottom = margin)
            }
            viewBinding.mainSideNav?.updatePaddingRelative(bottom = it)
        }
        mainViewModel.insetStart.observe(viewLifecycleOwner) {
            val startView = viewBinding.mainSideNav
            if (startView != null) {
                // let sideNav process start inset (in landscape layout)
                startView.updatePaddingRelative(start = it)
            } else {
                // let container process start inset (in portrait layout, break edge to edge)
                background?.updatePaddingRelative(start = it)
            }
        }
        mainViewModel.insetEnd.observe(viewLifecycleOwner) {
            // let container process end inset (break edge to edge)
            background?.updatePaddingRelative(end = it)
            // override navigation rail's automatic insets handling, todo remove after material 1.5
            lifecycleScope.launch(Dispatchers.Default) {
                delay(50)
                withContext(Dispatchers.Main) {
                    viewBinding.mainSideNav?.updatePaddingRelative(end = 0)
                }
            }
        }
        viewModel.navItemIndex
            .flowWithLifecycle(viewLifecycleOwner.lifecycle)
            .onEach { index ->
                val fragment = navFragments[index].value
                if (fragment.isAdded) return@onEach
                val containerId = viewBinding.mainFragmentContainer.id
                childFragmentManager.beginTransaction().replace(containerId, fragment).commit()
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun setupUi(context: Context) {
        isLand = viewBinding.mainLinear != null

        // below: app bar and drawer with navigation
//        val appBarConfiguration = AppBarConfiguration(navController.graph, mainDrawer)
//        mainTB.setupWithNavController(navController, appBarConfiguration)
//        mainBottomNav?.setupWithNavController(navController)

        val navBarListener = NavigationBarView.OnItemSelectedListener {
            val index = when (it.itemId) {
                R.id.mainNavUpdates -> 0
                R.id.mainNavUnits -> 1
                R.id.mainNavMore -> 2
                else -> 0
            }
            viewModel.setNavItemIndex(index)
            true
        }
        val mainNav = if (isLand == true) viewBinding.mainSideNav!! else viewBinding.mainBottomNav!!
        mainNav.setOnItemSelectedListener(navBarListener)
//        mainNav.selectedItemId = R.id.mainNavUpdates

        navScrollBehavior?.run {
            onSlidedUpCallback = up@{
                if (isNavUp || viewBinding.mainBottomNavContainer?.isGone == true) return@up
                isNavUp = true
                // hide bottom nav showing button
                viewBinding.mainShowBottomNav?.let {
                    animator.hideBottomNavShowing(it)
                }
                // change bottom content height
                viewModel.bottomContentWidth.value = bottomNavHeight
                // adjust nav bar
                val config = primaryNavBarConfig
                if (config != null && !config.isTransparentBar) {
                    val newConfig = SystemBarConfig(config.isDarkIcon, isTransparentBar = true)
                    activity?.window?.let {
                        SystemUtil.applyNavBarConfig(context, it, newConfig)
                    }
                }
            }
            onSlidedDownCallback = down@{
                if (!isNavUp) return@down
                isNavUp = false
                // show bottom nav showing button
                viewBinding.mainShowBottomNav?.let {
                    animator.showBottomNavShowing(it)
                }
                // change bottom content height
                viewModel.bottomContentWidth.value = mainViewModel.insetBottom.value
                // adjust nav bar
                primaryNavBarConfig?.let {
                    activity?.window?.let { w ->
                        SystemUtil.applyNavBarConfig(context, w, it)
                    }
                }
            }
        }
        // show bottom navigation bar once clicking the button
        viewBinding.mainShowBottomNav?.setOnClickListener { showNav() }
    }

    private val navScrollBehavior: MyHideBottomViewOnScrollBehavior<View>?
        get() {
            val nav = viewBinding.mainBottomNavContainer ?: return null
            val params = nav.layoutParams as CoordinatorLayout.LayoutParams
            return params.behavior as MyHideBottomViewOnScrollBehavior
        }

    private fun showNav() {
        // set visible explicitly
        viewBinding.mainBottomNavContainer?.isVisible = true
        navScrollBehavior?.slideUp(viewBinding.mainBottomNavContainer!!)
    }

    private fun hideNav() {
        // do not set visible explicitly, keep it untouched
        navScrollBehavior?.slideDown(viewBinding.mainBottomNavContainer!!)
    }

    private fun noNav() {
        navScrollBehavior?.slideDown(viewBinding.mainBottomNavContainer!!)
        // set gone
        viewBinding.mainBottomNavContainer?.isGone = true
    }
}

class MainPageViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val _navItemIndex: MutableStateFlow<Int> = MutableStateFlow(0)
    val navItemIndex: StateFlow<Int> = _navItemIndex
    val bottomContentWidth: MutableLiveData<Int> = MutableLiveData(0)

    fun setNavItemIndex(index: Int) {
        if (index < 0 || index == navItemIndex.value) return
        _navItemIndex.value = index
    }
}
