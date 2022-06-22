/*
 * Copyright 2022 Clifford Liu
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
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.Window
import androidx.activity.viewModels
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.view.updatePaddingRelative
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.madness.collision.Democratic
import com.madness.collision.R
import com.madness.collision.base.BaseActivity
import com.madness.collision.databinding.ActivityMainPageBinding
import com.madness.collision.diy.WindowInsets
import com.madness.collision.util.*
import com.madness.collision.util.controller.getSavedFragment
import com.madness.collision.util.controller.saveFragment
import com.madness.collision.util.os.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.reflect.KClass

inline fun <reified F: Fragment> Context.showPage(noinline data: (Bundle.() -> Unit)? = null) {
    showPage(F::class, data)
}

fun Context.showPage(klass: KClass<out Fragment>, data: (Bundle.() -> Unit)? = null) {
    val intent = Intent(this, MainPageActivity::class.java).apply {
        putExtra(MainPageActivity.ARG_PAGE, klass.qualifiedName)
        data?.let { putExtra(MainPageActivity.ARG_PAGE_DATA, Bundle().apply(it)) }
    }
    startActivity(intent)
}

fun Context.showPage(fragment: Fragment) {
    MainPageActivity.add(fragment)
    val intent = Intent(this, MainPageActivity::class.java)
    startActivity(intent)
}

class MainPageActivity : BaseActivity(), SystemBarMaintainerOwner {
    companion object {
        const val ARG_PAGE = "argPage"
        const val ARG_PAGE_DATA = "argPageData"
        private const val STATE_KEY_PAGE = "stateKeyPage"
        private val pages = ArrayDeque<Fragment>()

        fun add(fragment: Fragment) = synchronized(pages) { pages.add(fragment) }
    }

    private var _viewBinding: ActivityMainPageBinding? = null
    private val viewBinding: ActivityMainPageBinding get() = _viewBinding!!
    private val mainViewModel: MainViewModel by viewModels()
    private var pageFragment: Fragment? = null
    private var democratic: Democratic? = null
    override val systemBarMaintainer: SystemBarMaintainer = ActivitySystemBarMaintainer(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        _viewBinding = ActivityMainPageBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        setupUi()
        observeStates(this)
        setPage(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        pageFragment?.let { supportFragmentManager.saveFragment(outState, STATE_KEY_PAGE, it) }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        _viewBinding = null
        super.onDestroy()
    }

    private fun setPage(savedState: Bundle?) {
        val fMan = supportFragmentManager
        pageFragment = if (savedState != null) {
            fMan.getSavedFragment(savedState, STATE_KEY_PAGE)
        } else {
            val extras = intent?.extras
            if (extras != null) loadPage(extras) else loadPage()
        }
        val page = pageFragment ?: return
        if (page.isAdded) return
        fMan.beginTransaction().add(viewBinding.mainPageContainer.id, page).commitNowAllowingStateLoss()
    }

    private fun loadPage(extras: Bundle): Fragment? {
        val className = extras.getString(ARG_PAGE) ?: return null
        val pageData = extras.getBundle(ARG_PAGE_DATA)
        val page = Class.forName(className).declaredConstructors[0].newInstance() as Fragment
        pageData?.let { page.arguments = it }
        return page
    }

    private fun loadPage(): Fragment? {
        return synchronized(pages) { pages.removeFirstOrNull() }
    }

    private fun setupUi() {
        viewBinding.root.setOnApplyWindowInsetsListener { v, insets ->
            if (checkInsets(insets)) edgeToEdge(insets, false)
            val isRtl = if (v.isLayoutDirectionResolved) v.layoutDirection == View.LAYOUT_DIRECTION_RTL else false
            consumeInsets(WindowInsets(insets, isRtl))
            WindowInsetsCompat.CONSUMED.toWindowInsets()
        }
        viewBinding.mainPageToolbar.setOnMenuItemClickListener click@{ item ->
            item ?: return@click false
            try {
                democratic?.selectOption(item) ?: false
            } catch (e: Exception) {
                e.printStackTrace()
                notifyBriefly(R.string.text_error)
                false
            }
        }
    }

    private fun consumeInsets(insets: WindowInsets) {
        mainViewModel.updateInsetTop(insets.top)
        mainViewModel.insetBottom.value = insets.bottom
        mainViewModel.insetStart.value = insets.start
        mainViewModel.insetEnd.value = insets.end
    }

    private fun observeStates(context: Context) {
        val iconColor = ThemeUtil.getColor(context, R.attr.colorIcon)
        // for child fragment to access
        mainViewModel.democratic
            .flowWithLifecycle(lifecycle, Lifecycle.State.CREATED)
            .onEach {
                democratic = it
                clearDemocratic()
                viewBinding.mainPageToolbar.tag = viewBinding.mainPageToolbarDivider
                it.createOptions(this, viewBinding.mainPageToolbar, iconColor)
            }
            .launchIn(lifecycleScope)
        mainViewModel.page
            .flowWithLifecycle(lifecycle)
            .onEach {
                showPage(it.fragment)
                val shouldExit = it.args[1]
                if (shouldExit) finish()
            }
            .launchIn(lifecycleScope)
        mainViewModel.action.observe(this) {
            it ?: return@observe
            if (it.first.isBlank()) return@observe
            mainApplication.setAction(it)
            mainViewModel.action.value = "" to null
        }
        mainViewModel.insetTop.observe(this) {
            viewBinding.mainPageToolbar.run {
                updatePadding(top = it)
                measure()
                mainViewModel.contentWidthTop.value = measuredHeight
            }
        }
        mainViewModel.insetBottom.observe(this) {
            mainViewModel.contentWidthBottom.value = it
        }
        mainViewModel.insetStart.observe(this) {
            viewBinding.root.updatePaddingRelative(start = it)
        }
        mainViewModel.insetEnd.observe(this) {
            viewBinding.root.updatePaddingRelative(end = it)
        }
    }

    private fun clearDemocratic() {
        // Low profile mode is used in ApiDecentFragment. Deprecated since Android 11.
        if (X.belowOff(X.R)) disableLowProfileModeLegacy(window)
        viewBinding.mainPageToolbar.run {
            isVisible = true
            menu.clear()
            navigationIcon = null
            title = null
            setNavigationOnClickListener(null)
            setOnClickListener(null)
        }
        viewBinding.mainPageToolbarDivider.isVisible = true
    }

    @Suppress("deprecation")
    private fun disableLowProfileModeLegacy(window: Window) {
        val decorView = window.decorView
        decorView.systemUiVisibility = decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LOW_PROFILE.inv()
    }
}
