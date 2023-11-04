/*
 * Copyright 2020 Clifford Liu
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

package com.madness.collision.wearable.main

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.madness.collision.wearable.databinding.ActivityMainBinding
import com.madness.collision.wearable.misc.MiscMain
import com.madness.collision.wearable.util.P
import com.madness.collision.wearable.util.mainApplication
import kotlinx.coroutines.launch

internal class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var viewBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // handle splash screen transition
        installSplashScreen()

        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        applyInsets()

        lifecycleScope.launch {
            applyUpdates(this@MainActivity)
        }
    }

    /**
     * update notification availability, notification channels and check app update
     */
    private fun applyUpdates(context: Context){
        // enable notification
        mainApplication.notificationAvailable = NotificationManagerCompat.from(context).areNotificationsEnabled()
        MiscMain.clearCache(context)
        MiscMain.ensureUpdate(context, context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE))
    }

    private fun applyInsets() {
        val app = mainApplication
        viewBinding.mainContainer.setOnApplyWindowInsetsListener { _, insets ->
            app.insetBottom = insets.systemWindowInsetBottom
            viewModel.insetBottom.value = app.insetBottom
            insets
        }
    }
}
