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

package com.madness.collision.main

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.Window
import androidx.activity.viewModels
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.madness.collision.base.BaseActivity
import com.madness.collision.databinding.ActivityMainBinding
import com.madness.collision.diy.WindowInsets
import com.madness.collision.misc.MiscMain
import com.madness.collision.unit.Unit
import com.madness.collision.unit.api_viewing.AccessAV
import com.madness.collision.util.*
import com.madness.collision.util.controller.getSavedFragment
import com.madness.collision.util.controller.saveFragment
import com.madness.collision.util.notice.ToastUtils
import com.madness.collision.util.os.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

typealias MainFragment = androidx.fragment.app.Fragment

class MainActivity : BaseActivity(), SystemBarMaintainerOwner {

    private fun MainFragment(): MainFragment = AccessAV.getHomeFragment()

    companion object {
        /**
         * the activity to launch
         */
        const val LAUNCH_ACTIVITY = "launchActivity"
        /**
         * the target(fragment) to launch
         */
        const val LAUNCH_ITEM = "launchItem"
        const val LAUNCH_ITEM_ARGS = "launchItemArgs"

        const val ACTION_RECREATE = "mainRecreate"
        const val ACTION_EXTERIOR_THEME = "mainExteriorTheme"

        fun forItem(name: String, args: Bundle? = null): Bundle {
            val extras = Bundle()
            extras.putString(LAUNCH_ITEM, name)
            if (args != null) extras.putParcelable(LAUNCH_ITEM_ARGS, args)
            return extras
        }
    }

    // session data
    private var launchItem: String? = null
    private val viewModel: MainViewModel by viewModels()
    // views
    private lateinit var viewBinding: ActivityMainBinding
    // android
    private lateinit var mContext: Context
    private lateinit var mWindow: Window
    override val systemBarMaintainer: SystemBarMaintainer = ActivitySystemBarMaintainer(this)
    private var mainFragment: MainFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val context = this
        mContext = context

//        val elapsingSplash = ElapsingTime()
//        installSplashScreen().run {
//            setKeepVisibleCondition {
//                elapsingSplash.elapsed() < 3000
//            }
//        }

        val prefSettings = getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)

        // this needs to be invoked before update fragments are rendered
        viewModel.updateTimestamp()

        mWindow = window
        launchItem = intent?.getStringExtra(LAUNCH_ITEM)

        inflateLayout(context)

        lifecycleScope.launch(Dispatchers.Default) {
            initApplication(context, prefSettings)
            MiscMain.ensureUpdate(context, prefSettings)
            Unit.loadUnitClasses(context)

            withContext(Dispatchers.Main) {
                setupLayout(context, prefSettings, savedInstanceState)
            }

            checkMisc(context, prefSettings)
            checkTarget(context)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        mainFragment?.let { supportFragmentManager.saveFragment(outState, "mainF", it) }
        super.onSaveInstanceState(outState)
    }

    private fun initApplication(context: Context, prefSettings: SharedPreferences) {
        val app = mainApplication
        if (!app.dead) return
        app.debug = prefSettings.getBoolean(P.ADVANCED, false)
        app.dead = false
    }

    private fun inflateLayout(context: Context) {
        enableEdgeToEdge()

        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
    }

    private fun setupLayout(context: Context, prefSettings: SharedPreferences, savedState: Bundle?) {
        setupNav(savedState)
        setupViewModel(context, prefSettings)
        // invocation will clear stack
        setupUi(context)
    }

    private fun setupNav(savedState: Bundle?) {
        val fMan = supportFragmentManager
        val fragment = fMan.getSavedFragment(savedState, "mainF") ?: MainFragment()
        mainFragment = fragment
        if (launchItem != null) {
            fragment.arguments = Bundle().apply {
                putInt("mode" /* ARG_MODE */, 1 /* MODE_NO_UPDATES */)
            }
        }
        if (fragment.isAdded.not()) {
            val containerId = viewBinding.mainFragmentWrapper.id
            fMan.beginTransaction().replace(containerId, fragment).commitNowAllowingStateLoss()
        }
    }

    private fun setupViewModel(context: Context, prefSettings: SharedPreferences) {
        viewModel.page
            .flowWithLifecycle(lifecycle)
            .onEach {
                showPage(it.fragment)
                val shouldExit = it.args[1]
                if (shouldExit) finish()
            }
            .launchIn(lifecycleScope)
        viewModel.action.observe(this) {
            it ?: return@observe
            if (it.first.isBlank()) return@observe
            mainApplication.setAction(it)
            viewModel.action.value = "" to null
        }
    }

    private fun setupUi(context: Context) {
        checkTargetItem()

        viewBinding.root.setOnApplyWindowInsetsListener { v, insets ->
            if (checkInsets(insets)) edgeToEdge(insets, false)
            val isRtl = if (v.isLayoutDirectionResolved) v.layoutDirection == View.LAYOUT_DIRECTION_RTL else false
            consumeInsets(WindowInsets(insets, isRtl))
            insets
        }
    }

    private fun checkTargetItem() {
        val launchItemName = launchItem
        if (launchItemName.isNullOrEmpty()) return
        val itemArgs = intent?.getBundleExtra(LAUNCH_ITEM_ARGS)
        val hasArgs = itemArgs != null
        lifecycleScope.launch(Dispatchers.Default) {
            // wait for Unit init
            delay(200)
            launchItem = null
            val itemUnitDesc = Unit.getDescription(launchItemName) ?: return@launch
            withContext(Dispatchers.Main) {
                if (hasArgs) {
                    viewModel.displayUnit(itemUnitDesc.unitName, false, true, itemArgs)
                } else {
                    viewModel.displayUnit(itemUnitDesc.unitName, shouldExitAppAfterBack = true)
                }
            }
        }
    }

    /**
     * update notification availability, notification channels and check app update
     */
    private suspend fun checkMisc(context: Context, prefSettings: SharedPreferences) {
        // enable notification
        kotlin.run n@{
            // Abort check on Android 13
            if (OsUtils.satisfy(OsUtils.T)) return@n
            if (NotificationManagerCompat.from(context).areNotificationsEnabled()) return@n
            if (Random.nextInt(10) != 0) return@n
            withContext(Dispatchers.Main) {
                ToastUtils.popRequestNotification(this@MainActivity)
            }
        }
        MiscMain.registerNotificationChannels(context, prefSettings)

//        prHandler = PermissionRequestHandler(this)
//        SettingsFunc.check4Update(this, null, prHandler)
    }

    private suspend fun checkTarget(context: Context) {
        val activityName = intent.getStringExtra(LAUNCH_ACTIVITY) ?: ""
        if (activityName.isEmpty()) return
        val target = try {
            Class.forName(activityName)
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
            return
        }
        val startIntent = Intent(context, target)
        startIntent.putExtras(intent)
        delay(100)
        withContext(Dispatchers.Main) {
            startActivity(startIntent)
        }
    }

    override fun onDestroy() {
        val context = mContext
        AccessAV.clearContext()
        AccessAV.clearTags()
        MiscMain.clearCache(context)
        super.onDestroy()
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (SystemUtil.isSystemTvUi(this).not()) return super.onKeyUp(keyCode, event)
        val navKeyCode = SystemUtil.unifyTvNavKeyCode(keyCode)
        when(navKeyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> kotlin.Unit
            KeyEvent.KEYCODE_DPAD_DOWN -> kotlin.Unit
            KeyEvent.KEYCODE_DPAD_LEFT -> kotlin.Unit
            KeyEvent.KEYCODE_DPAD_RIGHT -> kotlin.Unit
            KeyEvent.KEYCODE_ENTER -> kotlin.Unit
            KeyEvent.KEYCODE_BACK -> kotlin.Unit
            KeyEvent.KEYCODE_HOME -> kotlin.Unit
            KeyEvent.KEYCODE_MENU -> kotlin.Unit
        }
        return super.onKeyUp(navKeyCode, event)
    }

    private fun consumeInsets(insets: WindowInsets) {
        val app = mainApplication
        app.insetTop = insets.top
        app.insetBottom = insets.bottom
        app.insetStart = insets.start
        app.insetEnd = insets.end
        viewModel.updateInsetTop(app.insetTop)
        viewModel.insetBottom.value = app.insetBottom
        viewModel.insetStart.value = app.insetStart
        viewModel.insetEnd.value = app.insetEnd
    }
}
