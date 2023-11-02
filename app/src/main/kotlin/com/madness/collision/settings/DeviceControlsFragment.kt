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

package com.madness.collision.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.activityViewModels
import com.madness.collision.Democratic
import com.madness.collision.R
import com.madness.collision.main.MainViewModel
import com.madness.collision.util.TaggedFragment
import com.madness.collision.util.mainApplication
import com.madness.collision.util.os.OsUtils

class DeviceControlsFragment : TaggedFragment(), Democratic {
    override val category: String = "Settings"
    override val id: String = "DeviceControls"

    private val mainViewModel: MainViewModel by activityViewModels()
    private var mutComposeView: ComposeView? = null
    private val composeView: ComposeView get() = mutComposeView!!

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        mainViewModel.configNavigation(toolbar, iconColor)
        toolbar.setTitle(R.string.app_device_controls)
        return true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = ComposeView(inflater.context)
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        mutComposeView = view
        return view
    }

    override fun onDestroyView() {
        mutComposeView = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        democratize(mainViewModel)
        val context = context ?: return
        val colorScheme = if (OsUtils.satisfy(OsUtils.S)) {
            val isDark = mainApplication.isDarkTheme
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            if (mainApplication.isDarkTheme) darkColorScheme() else lightColorScheme()
        }
        composeView.setContent {
            val insetTop = mainViewModel.contentWidthTop.observeAsState(0).value.toDp()
            val insetBottom = mainViewModel.contentWidthBottom.observeAsState(0).value.toDp()
            val paddingValues = remember(insetTop, insetBottom) {
                PaddingValues(top = insetTop, bottom = insetBottom)
            }
            MaterialTheme(colorScheme = colorScheme) {
                DeviceControlsPage(paddingValues = paddingValues)
            }
        }
    }

    @Composable
    private fun Int.toDp() = with(LocalDensity.current) { toDp() }
}
