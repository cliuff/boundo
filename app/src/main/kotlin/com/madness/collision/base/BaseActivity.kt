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

package com.madness.collision.base

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.*
import com.madness.collision.R
import com.madness.collision.main.AppAction
import com.madness.collision.main.MainActivity
import com.madness.collision.settings.LanguageMan
import com.madness.collision.util.P
import com.madness.collision.util.ThemeUtil
import com.madness.collision.util.mainApplication
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Base activity featuring custom language support.
 */
abstract class BaseActivity : AppCompatActivity() {
    private var themeId = P.SETTINGS_THEME_NONE
    private var lastActionData: Pair<String, Any>? = null

    override fun attachBaseContext(newBase: Context) {
        val context = LanguageMan(newBase).getLocaleContext()
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pref = getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
        themeId = loadThemeId(this, pref)
        mainApplication.action
            .flowWithLifecycle(lifecycle, Lifecycle.State.CREATED)
            .onEach { action ->
                val isStarted = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                if (isStarted) performAction(action) else suspendAction(action)
            }
            .launchIn(lifecycleScope)
    }

    private fun loadThemeId(context: Context, prefSettings: SharedPreferences) : Int {
        val actionData = lastActionData
        lastActionData = null
        if (actionData != null && actionData.first == MainActivity.ACTION_EXTERIOR_THEME) {
            val themeId = actionData.second as Int
            if (themeId != R.style.LaunchScreen) setTheme(themeId)
            ThemeUtil.updateIsDarkTheme(context, ThemeUtil.getIsDarkTheme(context))
            return themeId
        }
        return ThemeUtil.updateTheme(this, prefSettings)
    }

    private fun performAction(action: AppAction) {
        val context = this
        when (action.first) {
            MainActivity.ACTION_RECREATE -> recreate()
            MainActivity.ACTION_EXTERIOR_THEME -> {
                val pref = getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
                val newThemeId = ThemeUtil.updateTheme(context, pref, false)
                if (themeId == newThemeId) return
                lastActionData = MainActivity.ACTION_EXTERIOR_THEME to newThemeId
                recreate()
            }
        }
    }

    private fun suspendAction(action: AppAction) {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                owner.lifecycle.removeObserver(this)
                performAction(action)
            }
        })
    }
}
