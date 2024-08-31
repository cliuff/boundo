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
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import com.madness.collision.chief.app.ComposeFragment
import com.madness.collision.chief.app.rememberColorScheme
import com.madness.collision.diy.SpanAdapter
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.data.EasyAccess
import com.madness.collision.unit.api_viewing.list.AppPopOwner
import com.madness.collision.unit.api_viewing.list.pop
import com.madness.collision.unit.api_viewing.list.updateState
import com.madness.collision.unit.api_viewing.ui.info.AppInfoFragment
import com.madness.collision.util.hasUsageAccess

class AppUpdatesFragment : ComposeFragment(), AppInfoFragment.Callback {
    private val viewModel: AppUpdatesViewModel by viewModels()
    private var updatesCheckResumeTime: Long = 0L
    private var updatesCheckTime: Long = 0L
    private val popOwner = AppPopOwner()

    override fun getAppOwner(): AppInfoFragment.AppOwner {
        return object : AppInfoFragment.AppOwner {
            override val size: Int
                get() = viewModel.sectionsAppList.size

            override fun get(index: Int): ApiViewingApp? {
                return viewModel.sectionsAppList.getOrNull(index)
            }

            override fun getIndex(app: ApiViewingApp): Int {
                return viewModel.sectionsAppList.indexOf(app)
            }

            override fun findInAll(pkgName: String): ApiViewingApp? {
                return viewModel.sectionsAppList.find { it.packageName == pkgName }
                    ?: context?.let { context -> viewModel.getApp(context, pkgName) }
            }
        }
    }

    override fun onAppChanged(app: ApiViewingApp) {
        popOwner.updateState(app)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context?.let(EasyAccess::init)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.setUpdatesColumnCount(SpanAdapter.getSpanCount(this, 290f))
        if (savedInstanceState == null) {
            moderatedUpdatesCheck()
        }
    }

    override fun onResume() {
        super.onResume()
        if (lifecycleEventTime.compareValues(Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_CREATE) > 0
            && SystemClock.uptimeMillis() - updatesCheckResumeTime > 30_000) {
            updatesCheckResumeTime = SystemClock.uptimeMillis()
            moderatedUpdatesCheck()
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (!hidden && SystemClock.uptimeMillis() - updatesCheckResumeTime > 30_000) {
            updatesCheckResumeTime = SystemClock.uptimeMillis()
            moderatedUpdatesCheck()
        }
    }

    private fun moderatedUpdatesCheck() {
        if (SystemClock.uptimeMillis() - updatesCheckTime < 80) {
            Log.d("AppUpdatesFragment", "Abort updates check within 80ms.")
            return
        }
        updatesCheckTime = SystemClock.uptimeMillis()
        viewModel.checkUpdates(mainViewModel.timestamp, requireContext(), this)
    }

    @Composable
    override fun ComposeContent() {
        MaterialTheme(colorScheme = rememberColorScheme()) {
            AppUpdatesPage(
                paddingValues = rememberContentPadding(),
                eventHandler = rememberUpdatesEventHandler()
            )
        }
    }

    @Composable
    private fun rememberUpdatesEventHandler(): AppUpdatesEventHandler {
        return remember {
            object : AppUpdatesEventHandler {
                override fun hasUsageAccess(): Boolean {
                    return context?.hasUsageAccess == true
                }

                override fun showAppInfo(app: ApiViewingApp) {
                    popOwner.pop(this@AppUpdatesFragment, app)
                }

                override fun showAppListPage() {
                }

                override fun showUsageAccessSettings() {
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }

                override fun showAppSettings() {
                    // todo access app settings page
                }
            }
        }
    }
}
