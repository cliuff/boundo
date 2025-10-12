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
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.madness.collision.chief.app.ComposeFragment
import com.madness.collision.chief.app.ActivityPageNavController
import com.madness.collision.chief.app.LocalPageNavController
import com.madness.collision.ui.conn.ConnEventHandler
import com.madness.collision.ui.conn.ConnectionsPage
import com.madness.collision.ui.theme.MetaAppTheme
import com.madness.collision.util.mainApplication

class ConnectionsFragment : ComposeFragment(), AppHomeNavPage by AppHomeNavPageImpl() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setStatusBarDarkIcon(mainApplication.isPaleTheme)
    }

    @Composable
    override fun ComposeContent() {
        // enable new ComposePageActivity navigation from legacy pages
        val navController = remember { ActivityPageNavController(requireActivity()) }
        CompositionLocalProvider(LocalPageNavController provides navController) {
            MetaAppTheme {
                val eventHandler = rememberConnEventHandler(this)
                ConnectionsPage(eventHandler = eventHandler, contentPadding = navContentPadding)
            }
        }
    }
}

@Composable
private fun rememberConnEventHandler(homeNavPage: AppHomeNavPage) =
    remember<ConnEventHandler> {
        object : ConnEventHandler {
            override fun showAppSettings() {
                homeNavPage.mainAppHome?.showAppSettings()
            }
        }
    }
