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

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.widget.Toolbar
import androidx.core.animation.addListener
import androidx.core.view.isVisible
import androidx.core.view.updatePaddingRelative
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.madness.collision.Democratic
import com.madness.collision.R
import com.madness.collision.databinding.FragmentMainBinding
import com.madness.collision.main.updates.UpdatesFragment
import com.madness.collision.settings.SettingsFragment
import com.madness.collision.util.TaggedFragment
import com.madness.collision.util.ThemeUtil
import com.madness.collision.util.controller.getSavedFragment
import com.madness.collision.util.controller.saveFragment
import com.madness.collision.util.measure
import kotlin.math.roundToLong

class MainFragment : TaggedFragment(), Democratic, UpdatesFragment.Listener {
    override val category: String = "Main"
    override val id: String = "Main"

    companion object {
        private const val STATE_KEY_NAV_ITEM = "NavItem"
    }

    // session data
    private val viewModel: MainPageViewModel by viewModels()
    private val mainViewModel: MainViewModel by activityViewModels()
    // ui appearance data
    private var bottomNavHeight: Int = 0
    // views
    private lateinit var viewBinding: FragmentMainBinding
    private lateinit var navFragments: Array<Lazy<Fragment>>

    // this getter returns null before isLand is initialized, to avoid inconsistent value
    private val background: View? get() = viewBinding.mainFrame

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        toolbar.visibility = View.INVISIBLE
        (toolbar.tag as View?)?.isVisible = false  // hide toolbar divider
        return true
    }

    private var refreshToolAnimator: ObjectAnimator? = null

    override fun onRefreshState(isRefreshing: Boolean) {
        val toolItem = viewBinding.mainTB.menu.findItem(R.id.mainToolbarRefresh) ?: return
        if (toolItem.isVisible.not()) return
        val view = viewBinding.mainTB.findViewById<View>(R.id.mainToolbarRefresh) ?: return
        val animator = refreshToolAnimator
        when {
            isRefreshing && animator == null -> {
                val anim = ObjectAnimator.ofFloat(view, "rotation", 0f, 360f)
                refreshToolAnimator = anim.apply {
                    interpolator = LinearInterpolator()
                    repeatMode = ValueAnimator.RESTART
                    repeatCount = ValueAnimator.INFINITE
                    duration = 1000
                    addListener(onCancel = {
                        val progress = view.rotation % 360f
                        if (progress != 0f) {
                            val duration = ((360f - progress) * 1000f / 360f).roundToLong()
                            view.animate().setInterpolator(OvershootInterpolator())
                                .setDuration(duration).rotation(360f).start()
                        }
                    })
                    start()
                }
            }
            isRefreshing -> if (animator?.isStarted == false) animator.start()
            else -> animator?.cancel()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val updatesMode = arguments?.getInt(UpdatesFragment.ARG_MODE)
        val f = run f@{
            savedInstanceState ?: return@f null
            childFragmentManager.getSavedFragment<Fragment>(savedInstanceState, STATE_KEY_NAV_ITEM + 0)
        }
        navFragments = arrayOf(
            lazy {
                f ?: run {
                    Fragment()
                }
            },
        )
        val savedIndexes = listOf(f).mapIndexedNotNull { i, it -> it?.let { i } }
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
    }

    override fun onSaveInstanceState(outState: Bundle) {
        navFragments.forEachIndexed { i, lazy ->
            if (lazy.isInitialized().not()) return@forEachIndexed  // continue
            childFragmentManager.saveFragment(outState, STATE_KEY_NAV_ITEM + i, lazy.value)
        }
        super.onSaveInstanceState(outState)
    }

    private fun setupViewModel(context: Context) {
        democratize(mainViewModel)
        mainViewModel.insetTop.observe(viewLifecycleOwner) {
            viewBinding.mainTB.run {
                updatePaddingRelative(top = it)
                measure()
                mainViewModel.contentWidthTop.value = measuredHeight
            }
        }
        mainViewModel.insetBottom.observe(viewLifecycleOwner) {
            bottomNavHeight = it
            viewModel.bottomContentWidth.value = bottomNavHeight
        }
        mainViewModel.insetStart.observe(viewLifecycleOwner) {
            // let container process start inset (break edge to edge)
            background?.updatePaddingRelative(start = it)
        }
        mainViewModel.insetEnd.observe(viewLifecycleOwner) {
            // let container process end inset (break edge to edge)
            background?.updatePaddingRelative(end = it)
        }
    }

    private fun setupUi(context: Context) {
        val fragment = navFragments[0].value
        if (fragment.isAdded.not()) {
            val containerId = viewBinding.mainFragmentContainer.id
            childFragmentManager.beginTransaction().replace(containerId, fragment).commit()
        }

        val iconColor = ThemeUtil.getColor(context, R.attr.colorIcon)
        inflateAndTint(R.menu.toolbar_main, viewBinding.mainTB, iconColor)
        viewBinding.mainTB.setOnMenuItemClickListener(object : Toolbar.OnMenuItemClickListener {
            private var lastRefreshTime = -1L
            override fun onMenuItemClick(item: MenuItem?): Boolean {
                item ?: return false
                if (item.itemId == R.id.mainToolbarSettings) {
                    mainViewModel.displayFragment(SettingsFragment())
                    return true
                } else if (item.itemId == R.id.mainToolbarRefresh) {
                    val time = SystemClock.uptimeMillis()
                    if (time - lastRefreshTime < 500) return true
                    lastRefreshTime = time
                    (navFragments[0].value as? UpdatesFragment)?.refreshUpdates()
                    return true
                }
                return false
            }
        })
    }
}

class MainPageViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    val bottomContentWidth: MutableLiveData<Int> = MutableLiveData(0)
}
