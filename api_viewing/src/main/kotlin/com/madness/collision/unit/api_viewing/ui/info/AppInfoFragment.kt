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

package com.madness.collision.unit.api_viewing.ui.info

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.madness.collision.main.MainViewModel
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.list.AppInfoPage
import com.madness.collision.unit.api_viewing.list.LocalAppSwitcherHandler
import com.madness.collision.unit.api_viewing.ui.info.AppInfoFragment.AppOwner

class AppInfoFragment {
    interface Callback {
        fun getAppOwner(): AppOwner
        fun onAppChanged(app: ApiViewingApp) {}
    }

    interface AppOwner {
        val size: Int
        operator fun get(index: Int): ApiViewingApp?
        fun getIndex(app: ApiViewingApp): Int

        /**
         * In updates page and list filter, only a subset of apps are accessible.
         * This method should find [pkgName] in all apps available.
         */
        fun findInAll(pkgName: String): ApiViewingApp?
    }
}

    private fun createHandler(commCallback: AppInfoFragment.Callback?, getApp: () -> ApiViewingApp?, setApp: (ApiViewingApp?) -> Unit)
    : AppSwitcherHandler {
        return object : AppSwitcherHandler {
            private val ownerIndex: Pair<AppOwner, Int>? get() {
                val appOwner = commCallback?.getAppOwner() ?: return null
                val currentIndex = getApp()?.let { appOwner.getIndex(it) } ?: return null
                if (currentIndex < 0) return null
                return appOwner to currentIndex
            }

            override fun getPreviousPreview(): ApiViewingApp? {
                val (appOwner, currentIndex) = ownerIndex ?: return null
                val targetIndex = (currentIndex - 1).takeIf { it >= 0 }
                return targetIndex?.let { appOwner[it] }
            }

            override fun getNextPreview(): ApiViewingApp? {
                val (appOwner, currentIndex) = ownerIndex ?: return null
                val targetIndex = (currentIndex + 1).takeIf { it < appOwner.size }
                return targetIndex?.let { appOwner[it] }
            }

            override fun loadPrevious() {
                val (appOwner, currentIndex) = ownerIndex ?: return
                val targetIndex = (currentIndex - 1).takeIf { it >= 0 }
                if (targetIndex != null) {
                    val app = appOwner[targetIndex]
                    setApp(app)
                    if (app != null) commCallback?.onAppChanged(app)
                }
            }

            override fun loadNext() {
                val (appOwner, currentIndex) = ownerIndex ?: return
                val targetIndex = (currentIndex + 1).takeIf { it < appOwner.size }
                if (targetIndex != null) {
                    val app = appOwner[targetIndex]
                    setApp(app)
                    if (app != null) commCallback?.onAppChanged(app)
                }
            }

            override fun getApp(pkgName: String): ApiViewingApp? {
                val appOwner = commCallback?.getAppOwner() ?: return null
                return appOwner.findInAll(pkgName)
            }

            override fun loadApp(app: ApiViewingApp) {
                setApp(app)
                commCallback?.onAppChanged(app)
            }
        }
    }

    @Composable
    fun AppInfoPageContent(
        appInfoState: AppInfoSheetState,
        appInfoCallback: AppInfoFragment.Callback?,
        eventHandler: AppInfoEventHandler,
        colorScheme: ColorScheme,
    ) {
        val mainViewModel = viewModel<MainViewModel>()
        var switchPair: Pair<ApiViewingApp?, Int> by remember(appInfoState) {
            mutableStateOf(appInfoState.app to 0)
        }
        val switcherHandler = remember(appInfoCallback) {
            createHandler(appInfoCallback, { switchPair.first }, { app ->
                switchPair = app to switchPair.second + 1
                appInfoState.app = app
            })
        }

        MaterialTheme(colorScheme = colorScheme) {
            val a = switchPair.first
            if (a != null) {
                CompositionLocalProvider(LocalAppSwitcherHandler provides switcherHandler) {
                    val pageIds = remember(switchPair.second) {
                        val count = switchPair.second
                        if (count <= 0) listOf(count) else listOf(count - 1, count)
                    }
                    val vMap = remember { mutableStateMapOf<Int, Boolean>() }
                    Box() {
                        for (pId in pageIds) {
                            key(pId) {
                                AnimatedVisibility(
                                    visible = if (pageIds.size <= 1) true else vMap[pId] ?: false,
                                    enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)),
                                    exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessVeryLow)),
                                ) {
                                    AppInfoPage(a, mainViewModel, {
                                        appInfoState.app = null
                                        eventHandler.shareAppIcon(a)
                                    }, {
                                        appInfoState.app = null
                                        eventHandler.shareAppArchive(a)
                                    })
                                }
                                SideEffect {
                                    vMap[pId] = pId == switchPair.second
                                }
                            }
                        }
                    }
                }
            }
        }
    }
