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
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.viewModels
import com.madness.collision.R
import com.madness.collision.chief.app.ComposeViewOwner
import com.madness.collision.chief.app.rememberContentPadding
import com.madness.collision.main.showPage
import com.madness.collision.settings.DeviceControlsFragment
import com.madness.collision.ui.theme.MetaAppTheme
import com.madness.collision.unit.Unit
import com.madness.collision.util.os.OsUtils

class MyUnit : Unit() {

    override val id: String = "AT"

    private val viewModel: AtUnitViewModel by viewModels()
    private val composeViewOwner = ComposeViewOwner()
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
        return composeViewOwner.createView(inflater.context, viewLifecycleOwner)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        democratize()
        composeViewOwner.getView()?.setContent {
            val context = LocalContext.current
            MetaAppTheme {
                AudioTimerPage(
                    paddingValues = rememberContentPadding(mainViewModel),
                    onStartTimer = { requestTimer(context) },
                    onNavControls = { context.showPage<DeviceControlsFragment>() },
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
