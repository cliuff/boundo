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
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.madness.collision.R
import com.madness.collision.base.BaseActivity
import com.madness.collision.databinding.ActivityMainPageBinding
import com.madness.collision.diy.WindowInsets
import com.madness.collision.util.*
import com.madness.collision.util.controller.systemUi
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

class MainPageActivity : BaseActivity() {
    companion object {
        const val ARG_PAGE = "argPage"
        const val ARG_PAGE_DATA = "argPageData"
        private val pages = ArrayDeque<Fragment>()

        fun add(fragment: Fragment) = synchronized(pages) { pages.add(fragment) }
    }

    private var _viewBinding: ActivityMainPageBinding? = null
    private val viewBinding: ActivityMainPageBinding get() = _viewBinding!!
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefSettings = getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
        ThemeUtil.updateTheme(this, prefSettings)
        systemUi { fullscreen() }
        _viewBinding = ActivityMainPageBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        setupUi()
        observeStates(this)
        val extras = intent?.extras
        if (extras != null) loadPage(extras) else loadPage()
    }

    override fun onDestroy() {
        _viewBinding = null
        super.onDestroy()
    }

    private fun setupUi() {
        viewBinding.root.setOnApplyWindowInsetsListener { v, insets ->
            val isRtl = if (v.isLayoutDirectionResolved) v.layoutDirection == View.LAYOUT_DIRECTION_RTL else false
            consumeInsets(WindowInsets(insets, isRtl))
            insets
        }
        viewBinding.mainPageToolbar.setOnMenuItemClickListener click@{ item ->
            item ?: return@click false
            try {
                mainViewModel.democratic.value?.selectOption(item) ?: false
            } catch (e: Exception) {
                e.printStackTrace()
                notifyBriefly(R.string.text_error)
                false
            }
        }
    }

    private fun consumeInsets(insets: WindowInsets) {
        mainViewModel.insetTop.value = insets.top
        mainViewModel.insetBottom.value = insets.bottom
        mainViewModel.insetStart.value = insets.start
        mainViewModel.insetEnd.value = insets.end
    }

    private fun observeStates(context: Context) {
        val iconColor = ThemeUtil.getColor(context, R.attr.colorIcon)
        // for child fragment to access
        mainViewModel.democratic.observe(this) {
            clearDemocratic()
            viewBinding.mainPageToolbar.tag = viewBinding.mainPageToolbarDivider
            it.createOptions(this, viewBinding.mainPageToolbar, iconColor)
        }
        mainViewModel.unit.observe(this) {
            it ?: return@observe
            showPage(it.first)
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

    private fun loadPage(extras: Bundle) {
        val className = extras.getString(ARG_PAGE) ?: return
        val pageData = extras.getBundle(ARG_PAGE_DATA)
        val page = Class.forName(className).declaredConstructors[0].newInstance() as Fragment
        pageData?.let { page.arguments = it }
        supportFragmentManager.beginTransaction().add(viewBinding.mainPageContainer.id, page).commit()
    }

    private fun loadPage() {
        val page = synchronized(pages) { pages.removeFirstOrNull() } ?: return
        supportFragmentManager.beginTransaction().add(viewBinding.mainPageContainer.id, page).commit()
    }
}
