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

package com.madness.collision.unit.api_viewing.ui.upd

import android.content.Intent
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.os.BundleCompat
import androidx.fragment.app.viewModels
import com.madness.collision.BuildConfig
import com.madness.collision.chief.app.ComposeFragment
import com.madness.collision.chief.app.rememberColorScheme
import com.madness.collision.chief.auth.PermissionHandler
import com.madness.collision.chief.auth.PermissionState
import com.madness.collision.unit.api_viewing.apps.AppListPermission
import com.madness.collision.unit.api_viewing.ui.home.AppHomeNavPage
import com.madness.collision.unit.api_viewing.ui.home.AppHomeNavPageImpl
import com.madness.collision.unit.api_viewing.ui.info.AppInfoEventHandler
import com.madness.collision.unit.api_viewing.ui.info.rememberAppInfoEventHandler

class AppUpdatesFragment : ComposeFragment(),
    AppHomeNavPage by AppHomeNavPageImpl() {
    private val viewModel: AppUpdatesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { args ->
            // set initial content padding to use for avoiding visual flicker
            BundleCompat.getParcelable(args, AppHomeNavPage.ARG_CONTENT_PADDING, RectF::class.java)
                ?.run { navContentPadding = PaddingValues.Absolute(left.dp, top.dp, right.dp, bottom.dp) }
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (!hidden) viewModel.checkResumedUpdates(requireContext())
    }

    @Composable
    override fun ComposeContent() {
        MaterialTheme(colorScheme = rememberColorScheme()) {
            val appInfoEventHandler = rememberAppInfoEventHandler(this)
            AppUpdatesPage(
                paddingValues = navContentPadding,
                eventHandler = rememberUpdatesEventHandler(appInfoEventHandler)
            )
        }
    }

    private val getInstalledAppsHandler: PermissionHandler? =
        if (AppListPermission.GetInstalledAppsPkg != null) {
            PermissionHandler(this, AppListPermission.GetInstalledApps) { _, state ->
                viewModel.setAllPkgsQueryResult(state)
                if (state == PermissionState.Granted) {
                    viewModel.checkUpdates(requireContext())
                }
            }
        } else {
            null
        }

    private val appSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { }

    private fun requestSettingsChange() {
        val uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
        // as stated in the doc, avoid using Intent.FLAG_ACTIVITY_NEW_TASK with startActivityForResult()
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri)
        appSettingsLauncher.launch(intent)
    }

    @Composable
    private fun rememberUpdatesEventHandler(appInfoEventHandler: AppInfoEventHandler): AppUpdatesEventHandler {
        return remember {
            object : AppUpdatesEventHandler, AppInfoEventHandler by appInfoEventHandler {

                override fun setStatusBarDarkIcon(isDark: Boolean) {
                    this@AppUpdatesFragment.setStatusBarDarkIcon(isDark)
                }

                override fun showAppListPage() {
                }

                override fun showUsageAccessSettings() {
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }

                override fun requestAllPkgsQuery(permission: String?) {
                    val handler = getInstalledAppsHandler
                    if (viewModel.uiState.value.perm.canReqRuntimePerm && handler != null) {
                        handler.request()
                    } else {
                        requestSettingsChange()
                    }
                }

                override fun showAppSettings() {
                    mainAppHome?.showAppSettings()
                }

                @Composable
                override fun UnitBar(width: Dp) {
                    mainAppHome?.run { UnitBar(width) }
                }
            }
        }
    }
}
