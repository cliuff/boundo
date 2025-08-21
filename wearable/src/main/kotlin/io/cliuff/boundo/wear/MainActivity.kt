/*
 * Copyright 2025 Clifford Liu
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

package io.cliuff.boundo.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import io.cliuff.boundo.wear.conf.MiscMain
import io.cliuff.boundo.wear.ui.list.AppList
import io.cliuff.boundo.wear.ui.theme.MetaAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var appInitJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // handle splash screen transition
        installSplashScreen()

        super.onCreate(savedInstanceState)
        applyAppInit()

        setContent {
            MetaAppTheme {
                AppList()
            }
        }
    }

    private fun applyAppInit() {
        appInitJob?.cancel()
        appInitJob = lifecycleScope.launch(Dispatchers.Default) {
            val prefs = getSharedPreferences("PrefSettings", MODE_PRIVATE)
            MiscMain.applyAppVersionUpgrade(applicationContext, prefs)
        }
    }
}
