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

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentDialog
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.shape.MaterialShapeDrawable
import com.madness.collision.R
import com.madness.collision.chief.app.ComposeViewOwner
import com.madness.collision.chief.app.rememberColorScheme
import com.madness.collision.main.MainViewModel
import com.madness.collision.unit.api_viewing.apps.AppRepo
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.data.EasyAccess
import com.madness.collision.unit.api_viewing.list.AppInfoPage
import com.madness.collision.unit.api_viewing.list.AppListService
import com.madness.collision.unit.api_viewing.list.LocalAppSwitcherHandler
import com.madness.collision.unit.api_viewing.seal.SealMaker
import com.madness.collision.util.ThemeUtil
import com.madness.collision.util.configure
import com.madness.collision.util.mainApplication
import com.madness.collision.util.os.DialogFragmentSystemBarMaintainer
import com.madness.collision.util.os.SystemBarMaintainer
import com.madness.collision.util.os.SystemBarMaintainerOwner
import com.madness.collision.util.os.checkInsets
import com.madness.collision.util.os.edgeToEdge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AppInfoFragment() : BottomSheetDialogFragment(), SystemBarMaintainerOwner {
    interface Callback {
        fun getAppOwner(): AppOwner
        fun onAppChanged(app: ApiViewingApp)
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

    private val mainViewModel: MainViewModel by activityViewModels()
    private var infoApp: ApiViewingApp? = null
    private val composeViewOwner = ComposeViewOwner()
    override val systemBarMaintainer: SystemBarMaintainer = DialogFragmentSystemBarMaintainer(this)
    private val appPkgName: String get() = arguments?.getString(ARG_PKG_NAME) ?: ""
    private val commCallback: Callback? get() = (parentFragment ?: activity) as? Callback

    companion object {
        const val TAG = "AppInfoFragment"
        private const val ARG_PKG_NAME = "argPkgName"
    }

    constructor(pkgName: String) : this() {
        arguments = (arguments ?: Bundle()).apply {
            putString(ARG_PKG_NAME, pkgName)
        }
    }

    constructor(app: ApiViewingApp) : this(app.packageName) {
        infoApp = app
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // BottomSheetDialog style, set enableEdgeToEdge to true
        // (and navigationBarColor set to transparent or translucent)
        // to disable automatic insets handling
        setStyle(STYLE_NORMAL, R.style.AppTheme_Pop)
    }

    override fun onStart() {
        super.onStart()
        val context = context ?: return
        val rootView = view ?: return
        BottomSheetBehavior.from(rootView.parent as View).configure(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return composeViewOwner.createView(inflater.context, viewLifecycleOwner)
    }

    private fun setBackgroundColor(color: Int) {
        val sheetDialog = dialog as? BottomSheetDialog? ?: return
        val klass = BottomSheetBehavior::class.java
        try {
            val method = klass.getDeclaredMethod("getMaterialShapeDrawable")
            method.isAccessible = true
            val shape = method.invoke(sheetDialog.behavior) as? MaterialShapeDrawable?
            shape?.setTint(color)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.setOnApplyWindowInsetsListener { _, insets ->
            if (checkInsets(insets)) edgeToEdge(insets, false)
            WindowInsetsCompat.CONSUMED.toWindowInsets()!!
        }
        composeViewOwner.getView()?.setContent {
            AppInfoPageContent(colorScheme = rememberColorScheme())
        }
    }

    private fun createHandler(getApp: () -> ApiViewingApp?, setApp: (ApiViewingApp?) -> Unit)
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

    // todo move to view model
    private fun getApp(context: Context, pkgName: String): ApiViewingApp? {
        return AppRepo.dumb(context).getApp(pkgName)
    }

    @Composable
    private fun AppInfoPageContent(colorScheme: ColorScheme) {
        var switchPair: Pair<ApiViewingApp?, Int> by remember { mutableStateOf(infoApp to 0) }
        val pkgName = remember(switchPair) { switchPair.first?.packageName }
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        if (switchPair.first == null) {
            SideEffect {
                scope.launch(Dispatchers.Default) {
                    val a = getApp(context, appPkgName)
                    switchPair = a to switchPair.second
                }
            }
        }
        val switcherHandler = remember(Unit) {
            createHandler({ switchPair.first }, { switchPair = it to switchPair.second + 1 })
        }
        MaterialTheme(colorScheme = colorScheme) {
            val a = switchPair.first
            if (a != null) {
                LaunchedEffect(pkgName) {
                    val color = when {
                        EasyAccess.isSweet -> SealMaker.getItemColorBack(context, a.targetAPI)
                        mainApplication.isPaleTheme -> 0xFFF7FFE9.toInt()
                        else -> ThemeUtil.getColor(context, R.attr.colorASurface)
                    }
                    setBackgroundColor(color)
                }
                // use parent instead of child fragment manager to show dialog after dismiss()
                val fMan = remember { parentFragmentManager }
                // provide dialog's onBackPressedDispatcher to implement custom back handler
                val providedBackDispatcher = remember dis@{
                    val dia = dialog ?: return@dis null
                    if (dia !is ComponentDialog) return@dis null
                    LocalOnBackPressedDispatcherOwner provides dia
                }
                val providedSwitcherHandler = LocalAppSwitcherHandler provides switcherHandler
                val providedValues = arrayOf(providedBackDispatcher, providedSwitcherHandler)
                    .filterNotNull().toTypedArray()
                CompositionLocalProvider(*providedValues) {
                    val fragment = this
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
                                    AppInfoPage(a, mainViewModel, fragment, {
                                        dismiss()
                                        AppListService().actionIcon(context, a, fMan)
                                    }, {
                                        dismiss()
                                        // calling dismiss() cancels composable scope
                                        val apkScope = CoroutineScope(Job())
                                        AppListService().actionApk(context, a, apkScope, fMan)
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
}
