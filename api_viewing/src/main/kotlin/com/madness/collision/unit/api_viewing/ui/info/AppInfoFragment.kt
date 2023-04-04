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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentDialog
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.shape.MaterialShapeDrawable
import com.madness.collision.R
import com.madness.collision.main.MainViewModel
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.data.EasyAccess
import com.madness.collision.unit.api_viewing.database.DataMaintainer
import com.madness.collision.unit.api_viewing.list.AppInfoPage
import com.madness.collision.unit.api_viewing.list.AppListService
import com.madness.collision.unit.api_viewing.seal.SealMaker
import com.madness.collision.util.ThemeUtil
import com.madness.collision.util.configure
import com.madness.collision.util.mainApplication
import com.madness.collision.util.os.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AppInfoFragment() : BottomSheetDialogFragment(), SystemBarMaintainerOwner {
    private val mainViewModel: MainViewModel by activityViewModels()
    private var infoApp: ApiViewingApp? = null
    private var _composeView: ComposeView? = null
    private val composeView: ComposeView get() = _composeView!!
    override val systemBarMaintainer: SystemBarMaintainer = DialogFragmentSystemBarMaintainer(this)
    private val appPkgName: String get() = arguments?.getString(ARG_PKG_NAME) ?: ""

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
        _composeView = ComposeView(inflater.context)
        return composeView
    }

    override fun onDestroyView() {
        _composeView = null
        super.onDestroyView()
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
        val context = context ?: return
        view.setOnApplyWindowInsetsListener { _, insets ->
            if (checkInsets(insets)) edgeToEdge(insets, false)
            WindowInsetsCompat.CONSUMED.toWindowInsets()!!
        }
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        val colorScheme = if (OsUtils.satisfy(OsUtils.S)) {
            if (mainApplication.isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            if (mainApplication.isDarkTheme) darkColorScheme() else lightColorScheme()
        }
        composeView.setContent {
            AppInfoPageContent(colorScheme)
        }
    }

    @Composable
    private fun AppInfoPageContent(colorScheme: ColorScheme) {
        var app: ApiViewingApp? by remember { mutableStateOf(infoApp) }
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        if (app == null) {
            val lifecycleOwner = this
            SideEffect {
                scope.launch(Dispatchers.Default) {
                    app = DataMaintainer.get(context, lifecycleOwner).selectApp(appPkgName)
                }
            }
        }
        MaterialTheme(colorScheme = colorScheme) {
            app?.let { a ->
                SideEffect {
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
                val providedValues = arrayOf(providedBackDispatcher).filterNotNull().toTypedArray()
                CompositionLocalProvider(*providedValues) {
                    AppInfoPage(a, mainViewModel, this, {
                        dismiss()
                        AppListService().actionIcon(context, a, fMan)
                    }, {
                        dismiss()
                        // calling dismiss() cancels composable scope
                        val apkScope = CoroutineScope(Job())
                        AppListService().actionApk(context, a, apkScope, fMan)
                    })
                }
            }
        }
    }
}
