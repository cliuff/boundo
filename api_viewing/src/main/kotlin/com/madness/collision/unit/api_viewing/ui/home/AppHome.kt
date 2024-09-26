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
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.waterfall
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.List
import androidx.compose.material.icons.twotone.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailDefaults
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.compose.AndroidFragment
import androidx.fragment.compose.FragmentState
import androidx.fragment.compose.rememberFragmentState
import com.madness.collision.chief.app.ComposeFragment
import com.madness.collision.chief.app.rememberColorScheme
import com.madness.collision.util.dev.PreviewCombinedColorLayout

class AppHomeFragment : ComposeFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // create empty view for legacy NoUpdatesMode
        if (arguments?.getInt("mode") == 1) return View(inflater.context)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    @Composable
    override fun ComposeContent() {
        MaterialTheme(colorScheme = rememberColorScheme()) {
            AppHomePage()
        }
    }
}

interface AppHomeNav {
    fun setNavPage(index: Int)
    fun navBack()
}

@Composable
fun AppHomePage() {
    var selNavIndex by rememberSaveable { mutableIntStateOf(0) }
    val (homeNav, setHomeNav) = remember { mutableStateOf<AppHomeNav?>(null) }
    if (selNavIndex != 0 && homeNav != null) {
        BackHandler { selNavIndex = 0; homeNav.navBack() }
    }
    val homeInsets = WindowInsets.systemBars.union(WindowInsets.waterfall)
    // todo retain nav fragment across layout changes
    val navFragmentState = rememberFragmentState()
    BoxWithConstraints {
        // window size classes, expanded: rail, medium/compact: rail in landscape
        // prefer nav rail in split screen mode (50dp bonus size)
        if (maxWidth >= 840.dp || maxWidth + 50.dp >= maxHeight) {
            Row() {
                HomeNavigationRail(
                    selectedIndex = selNavIndex,
                    onSelectItem = { i -> selNavIndex = i; homeNav?.setNavPage(i) },
                    windowInsets = homeInsets
                        .only(WindowInsetsSides.Vertical + WindowInsetsSides.Start),
                )
                Box {
                    val contentInsets = homeInsets
                        .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                    val contentPadding = contentInsets.asPaddingValues()
                    HomeNavFragment(navFragmentState, contentPadding, setHomeNav)
                }
            }
        } else {
            Scaffold(
                bottomBar = {
                    HomeNavigationBar(
                        selectedIndex = selNavIndex,
                        onSelectItem = { i -> selNavIndex = i; homeNav?.setNavPage(i) },
                        windowInsets = homeInsets
                            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
                    )
                },
                containerColor = Color.Transparent,
                contentWindowInsets = homeInsets,
                content = { contentPadding ->
                    HomeNavFragment(navFragmentState, contentPadding, setHomeNav)
                }
            )
        }
    }
}

@Composable
private fun HomeNavFragment(
    fragmentState: FragmentState = rememberFragmentState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onUpdate: (AppHomeNavFragment) -> Unit,
) {
    var navFragment: AppHomeNavFragment? by remember { mutableStateOf(null) }
    AndroidFragment<AppHomeNavFragment>(
        modifier = Modifier.fillMaxSize(),
        fragmentState = fragmentState,
        onUpdate = { navFgm ->
            onUpdate(navFgm)
            navFragment = navFgm
        }
    )
    LaunchedEffect(navFragment, contentPadding) {
        navFragment?.setContentPadding(contentPadding)
    }
}

@Composable
private fun HomeNavigationBar(
    modifier: Modifier = Modifier,
    selectedIndex: Int,
    onSelectItem: (Int) -> Unit,
    windowInsets: WindowInsets = NavigationBarDefaults.windowInsets,
) {
    NavigationBar(modifier = modifier, windowInsets = windowInsets) {
        NavigationBarItem(
            selected = selectedIndex == 0,
            onClick = { onSelectItem(0) },
            icon = { Icon(Icons.TwoTone.Home, contentDescription = null) }
        )
        NavigationBarItem(
            selected = selectedIndex == 1,
            onClick = { onSelectItem(1) },
            icon = { Icon(Icons.AutoMirrored.TwoTone.List, contentDescription = null) }
        )
    }
}

@Composable
private fun HomeNavigationRail(
    modifier: Modifier = Modifier,
    selectedIndex: Int,
    onSelectItem: (Int) -> Unit,
    windowInsets: WindowInsets = NavigationRailDefaults.windowInsets,
) {
    NavigationRail(modifier = modifier, windowInsets = windowInsets) {
        Spacer(modifier = Modifier.weight(1f))
        NavigationRailItem(
            selected = selectedIndex == 0,
            onClick = { onSelectItem(0) },
            icon = { Icon(Icons.TwoTone.Home, contentDescription = null) }
        )
        NavigationRailItem(
            selected = selectedIndex == 1,
            onClick = { onSelectItem(1) },
            icon = { Icon(Icons.AutoMirrored.TwoTone.List, contentDescription = null) }
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

@PreviewCombinedColorLayout
@Composable
private fun AppHomePreview() {
    MaterialTheme {
        Scaffold(
            bottomBar = { HomeNavigationBar(selectedIndex = 0, onSelectItem = {}) },
            content = { _ -> },
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun AppHomeNavRailPreview() {
    MaterialTheme {
        Row() {
            HomeNavigationRail(selectedIndex = 0, onSelectItem = {})
            Box(modifier = Modifier.fillMaxSize())
        }
    }
}
