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

package com.madness.collision.unit.api_viewing.ui.pref

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.activityViewModels
import com.madness.collision.Democratic
import com.madness.collision.main.MainViewModel
import com.madness.collision.util.TaggedFragment
import com.madness.collision.util.mainApplication
import com.madness.collision.util.os.OsUtils

class DiffHistoryFragment : TaggedFragment(), Democratic {
    override val category: String = "AV"
    override val id: String = "DiffHistory"

    private var mutableComposeView: ComposeView? = null
    private val composeView: ComposeView get() = mutableComposeView!!
    private val mainViewModel: MainViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val composeView = ComposeView(inflater.context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }
        mutableComposeView = composeView
        return composeView
    }

    override fun onDestroyView() {
        mutableComposeView = null
        super.onDestroyView()
    }

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        mainViewModel.configNavigation(toolbar, iconColor)
        toolbar.setTitle("Diff History")
        return true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        democratize(mainViewModel)
        val context = context ?: return
        val colorScheme = if (OsUtils.satisfy(OsUtils.S)) {
            if (mainApplication.isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            if (mainApplication.isDarkTheme) darkColorScheme() else lightColorScheme()
        }
        composeView.setContent {
            MaterialTheme(colorScheme = colorScheme) {
                DiffHistoryPage()
            }
        }
    }
}