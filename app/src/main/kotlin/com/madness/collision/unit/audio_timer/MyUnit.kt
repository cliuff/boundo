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

package com.madness.collision.unit.audio_timer

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.viewModels
import com.madness.collision.R
import com.madness.collision.unit.Unit
import com.madness.collision.util.mainApplication
import com.madness.collision.util.os.OsUtils

class MyUnit : Unit() {

    override val id: String = "AT"

    private val viewModel: AtUnitViewModel by viewModels()
    private var mutComposeView: ComposeView? = null
    private val composeView: ComposeView get() = mutComposeView!!
    private lateinit var timerController: AudioTimerController

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        configNavigation(toolbar, iconColor)
        toolbar.setTitle(R.string.unit_audio_timer)
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        timerController = AudioTimerController(requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val composeView = ComposeView(inflater.context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }
        mutComposeView = composeView
        return composeView
    }

    override fun onDestroyView() {
        mutComposeView = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        democratize()
        val context = context ?: return
        val colorScheme = if (OsUtils.satisfy(OsUtils.S)) {
            val isDark = mainApplication.isDarkTheme
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            if (mainApplication.isDarkTheme) darkColorScheme() else lightColorScheme()
        }
        composeView.setContent {
            MaterialTheme(colorScheme = colorScheme) {
                AudioTimerPage(
                    mainViewModel = mainViewModel,
                    onStartTimer = { requestTimer(context) },
                )
            }
        }
    }

    private fun requestTimer(context: Context) {
        when {
            // request runtime permission on Android 13
            OsUtils.dissatisfy(OsUtils.T) -> startTimer()
            NotificationManagerCompat.from(context).areNotificationsEnabled() -> startTimer()
            else -> postNotificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private val postNotificationsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()) register@{ granted ->
        if (!granted) return@register
        startTimer()
    }

    private fun startTimer() {
        timerController.startTimer(viewModel.uiState.value)
    }
}
