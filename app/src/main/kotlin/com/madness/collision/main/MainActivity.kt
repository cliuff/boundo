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

package com.madness.collision.main

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.widget.Checkable
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.NotificationManagerCompat
import androidx.core.os.postDelayed
import androidx.core.view.get
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.observe
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.radiobutton.MaterialRadioButton
import com.madness.collision.Democratic
import com.madness.collision.R
import com.madness.collision.diy.WindowInsets
import com.madness.collision.misc.MiscMain
import com.madness.collision.settings.SettingsFunc
import com.madness.collision.unit.Unit
import com.madness.collision.util.*
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.*
import kotlin.random.Random

class MainActivity : AppCompatActivity(), Toolbar.OnMenuItemClickListener {
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
        /**
         * update exterior after background has changed
         */
        const val ACTION_EXTERIOR = "mainExterior"
        const val ACTION_EXTERIOR_THEME = "mainExteriorTheme"

        var themeId = P.SETTINGS_THEME_NONE

        var mainBottomNavRef: WeakReference<View>? = null
            private set

        fun forItem(name: String, args: Bundle? = null): Bundle {
            val extras = Bundle()
            extras.putString(LAUNCH_ITEM, name)
            if (args != null) extras.putParcelable(LAUNCH_ITEM_ARGS, args)
            return extras
        }
    }

    private var primaryNavBarConfig: SystemBarConfig? = null
    private var isToolbarInflated = false
    private var colorIcon = 0
    private var isLand = false
    private val viewModel: MainViewModel by viewModels()
    private lateinit var navController: NavController
    private lateinit var navHostFragment: NavHostFragment
    private val backPressedCallbackStack: Stack<BackwardOperation>
        get() = viewModel.backPressedCallbackStack
    private var backupCallback: Stack<BackwardOperation>? = null
    private var shouldKeepBackup: Boolean = false
//    private var shouldCareBack: Boolean = false
    internal lateinit var background: View
    private lateinit var mContext: Context
    private lateinit var prefSettings: SharedPreferences
    private lateinit var mWindow: Window

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = this
        prefSettings = getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
        updateThemeId(mContext)

        initData(mContext)
        mWindow = window
        viewModel.updateTimestamp()
        applyEssentials(mContext)
        applyUI()
        applyMisc(mContext)
    }

    private fun applyEssentials(context: Context) {
        GlobalScope.launch {
            MiscMain.ensureUpdate(context, prefSettings)
            Unit.loadUnitClasses(context)
        }
    }

    override fun onDestroy() {
        backPressedCallbackStack.forEach {
            it.cachedCallback?.remove()
            it.cachedCallback = null
        }
        GlobalScope.launch {
            MiscMain.clearCache(this@MainActivity)
        }
        super.onDestroy()
    }

    private fun updateThemeId(context: Context) {
        if (mainApplication.globalValue is Pair<*, *>) {
            val globalValue = mainApplication.globalValue as Pair<*, *>
            if (globalValue.first as String == ACTION_EXTERIOR_THEME) {
                themeId = globalValue.second as Int
                if (themeId != R.style.LaunchScreen) setTheme(themeId)
                ThemeUtil.updateIsDarkTheme(context, ThemeUtil.getIsDarkTheme(context))
                mainApplication.globalValue = null
            } else themeId = ThemeUtil.updateTheme(this, prefSettings)
        } else themeId = ThemeUtil.updateTheme(this, prefSettings)
    }

    private fun initData(context: Context) {
        if (mainApplication.dead) {
            mainApplication.debug = prefSettings.getBoolean(P.ADVANCED, false)
            initApplication(context)
        }
    }

    private fun applyUI() {
        mWindow.let { SystemUtil.applyEdge2Edge(it) }

        SettingsFunc.updateLanguage(mContext)
        setContentView(R.layout.fragment_main)

        viewModel.navViewRef = WeakReference(mainSideNav)
        navHostFragment = supportFragmentManager.findFragmentById(R.id.mainNavHost) as NavHostFragment
        navController = navHostFragment.navController

        setupViewModel()

        if (!backPressedCallbackStack.empty()) {
            backupCallback = backPressedCallbackStack
            viewModel.backPressedCallbackStack = Stack()
            shouldKeepBackup = true
        }
        // invoke will clear stack
        applyResources()

        viewModel.background.observe(this) {
            applyExterior()
            if (isToolbarInflated || !mainApplication.exterior) applyColor()
        }
    }

    override fun onBackPressed() {
        // across configuration change
        // fragment manager added callbacks but they do not work properly
        if (backupCallback != null) {
            viewModel.backPressedCallbackStack = backupCallback!!
            backupCallback = null
            setupOnBackPressedDispatcher()
//            shouldCareBack = true
        }
        // work around for those callbacks
//        if (shouldCareBack && backPressedCallbackStack.empty()) {
//            goAllTheWayBack()
//            goAllTheWayBack()
//            return
//        }
        super.onBackPressed()
    }

    private fun setupOnBackPressedDispatcher() {
        if (backPressedCallbackStack.empty()) return
        val last = backPressedCallbackStack.peek()
        backPressedCallbackStack.forEach {
            onBackPressedDispatcher.addCallback(it.makeCallback(it === last))
        }
    }

    private fun BackwardOperation.makeCallback(isForwardTheTop: Boolean = false): OnBackPressedCallback {
        val shouldShowNavAfterBack = operationFlags[0]
        val shouldExitAppAfterBack = operationFlags[1]
        val isMoreUnit = !backPressedCallbackStack.empty()
        val navFm = navHostFragment.childFragmentManager
        val forwardFragment = if (isForwardTheTop) navFm.fragments.find { f -> f.isVisible } else if (forwardPage.hasRef) forwardPage.fragment else null
        val backwardFragment = backwardPage.fragment
        cachedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                minusBackPressedCallback(this)
                if (shouldExitAppAfterBack) {
                    finish()
                    return
                }
                navFm.beginTransaction().animRetreat.run {
                    if (forwardFragment != null) remove(forwardFragment)
                    if (backwardFragment.isAdded) show(backwardFragment)
                    else add(navHostFragment.view?.id ?: 0, backwardFragment)
                    commitNow()
                }
                if (backwardFragment is Democratic) backwardFragment.democratize(viewModel)
                if (shouldShowNavAfterBack) showNav()
                // make unit value matches reality
                viewModel.unit.value = if (isMoreUnit) backwardFragment to booleanArrayOf(false, false) else null
            }
        }
        return cachedCallback!!
    }

    private fun minusBackPressedCallback(callback: OnBackPressedCallback) {
        if (backPressedCallbackStack.empty()) return
        if (backPressedCallbackStack.peek().cachedCallback === callback)
            backPressedCallbackStack.pop().cachedCallback?.remove()
    }

    private fun clearBackPressedCallback() {
        backPressedCallbackStack.forEach {
            it.cachedCallback?.remove()
        }
        backPressedCallbackStack.clear()
    }

    private fun applyMisc(context: Context) {
        GlobalScope.launch {
            applyUpdates(context)

            val activityName = intent.getStringExtra(LAUNCH_ACTIVITY) ?: ""
            val target = if (activityName.isNotEmpty()) Class.forName(activityName) else null
            target?.let {
                val startIntent = Intent(this@MainActivity, it)
                startIntent.putExtras(intent)
                launch(Dispatchers.Main) { startActivity(startIntent) }
            }
        }
    }

    /**
     * update notification availability, notification channels and check app update
     */
    private fun applyUpdates(context: Context) {
        // enable notification
        mainApplication.notificationAvailable = NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (!mainApplication.notificationAvailable && Random(System.currentTimeMillis()).nextInt(10) == 0) {
            GlobalScope.launch(Dispatchers.Main) {
                X.popRequestNotification(this@MainActivity)
            }
        }
        MiscMain.registerNotificationChannels(context, prefSettings)
        MiscMain.clearCache(context)

//        prHandler = PermissionRequestHandler(this)
//        SettingsFunc.check4Update(this, null, prHandler)
    }

    private fun initApplication(context: Context) {
        val application = mainApplication
        GlobalScope.launch {
            application.statusBarHeight = X.getStatusBarHeight(context)
            if (!mainApplication.isDarkTheme) MiscMain.updateExteriorBackgrounds(context)
            application.dead = false
        }
    }

    private fun clearDemocratic() {
        mainTB.menu.clear()
        mainTB.setOnClickListener(null)
    }

    private val FragmentTransaction.animNew: FragmentTransaction
        get() = disallowAddToBackStack().setCustomAnimations(
                R.anim.fragment_open_enter, R.anim.fragment_fade_exit,
                R.anim.fragment_fade_enter, R.anim.fragment_close_exit
        )

    private val FragmentTransaction.animRetreat: FragmentTransaction
        get() = setCustomAnimations(
                R.anim.fragment_fade_enter, R.anim.fragment_close_exit,
                R.anim.fragment_fade_enter, R.anim.fragment_close_exit
        )

    private fun setupViewModel() {
        viewModel.democratic.observe(this) {
            clearDemocratic()
            it.createOptions(mContext, mainTB, colorIcon)
        }
        viewModel.unit.observe(this) {
            it ?: return@observe
            val navFm = navHostFragment.childFragmentManager
            navFm.fragments.find { f -> f.isVisible }?.also { topFragment ->
                val (unitFragment, flags) = it
                if (topFragment == unitFragment) return@also
                val operation = BackwardOperation(unitFragment, topFragment, flags)
                backPressedCallbackStack.push(operation)
                onBackPressedDispatcher.addCallback(operation.makeCallback())
                navFm.beginTransaction().animNew.hide(topFragment).add(navHostFragment.view?.id ?: 0, unitFragment).commit()
                hideNav()
            }
        }
        viewModel.popUpBackStackFun = { isFromNav, shouldShowNavAfterBack ->
            when {
                onBackPressedDispatcher.hasEnabledCallbacks() -> onBackPressedDispatcher.onBackPressed()
                isFromNav -> navController.popBackStack()
                else -> supportFragmentManager.popBackStack()
            }
            if (shouldShowNavAfterBack) showNav()
        }
        viewModel.action.observe(this) {
            when (it.first) {
                "" -> return@observe
                ACTION_EXTERIOR -> updateExterior()
                ACTION_RECREATE -> recreate()
                ACTION_EXTERIOR_THEME -> {
                    val newThemeId = ThemeUtil.updateTheme(mContext, prefSettings, false)
                    if (themeId != newThemeId) {
                        mainApplication.globalValue = ACTION_EXTERIOR_THEME to newThemeId
                        recreate()
                    }
                }
            }
            viewModel.action.value = "" to null
        }
        viewModel.insetTop.observe(this) {
            mainTB.apply {
                alterPadding(top = it)
                measure()
                viewModel.contentWidthTop.value = measuredHeight
            }
            mainSideNav?.alterPadding(top = it)
        }
        viewModel.insetBottom.observe(this) {
            viewModel.contentWidthBottom.value = mainBottomNav?.run {
                alterPadding(bottom = it)
                measure()
                measuredHeight
            } ?: it
            mainSideNav?.alterPadding(bottom = it)
        }
        viewModel.insetLeft.observe(this) {
            background.alterPadding(start = it)
            mainSideNav?.alterPadding(start = it)
        }
        viewModel.insetRight.observe(this) {
            background.alterPadding(end = it)
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        item ?: return false
        return try {
            viewModel.democratic.value?.selectOption(item) ?: false
        } catch (e: Exception) {
            notifyBriefly(R.string.text_error)
            false
        }
    }

    private fun applyResources() {
        mainLinear?.also { isLand = true }
        background = if (isLand) mainLinear!! else mainFrame!!

        colorIcon = ThemeUtil.getColor(mContext, R.attr.colorIcon)

        // below: app bar and drawer with navigation
//        val appBarConfiguration = AppBarConfiguration(navController.graph, mainDrawer)
//        mainTB.setupWithNavController(navController, appBarConfiguration)
//        mainBottomNav?.setupWithNavController(navController)

        mainBottomNav?.setOnNavigationItemSelectedListener {
            mainNav(it.itemId)
            true
        }
        mainBottomNavRef = WeakReference(mainBottomNav)
        navScrollBehavior?.run {
            onSlidedUpCallback = {
                val config = primaryNavBarConfig
                if (config != null && !config.isTransparentBar)
                    SystemUtil.applyNavBarConfig(mContext, mWindow, SystemBarConfig(config.isDarkIcon, isTransparentBar = true))
            }
            onSlidedDownCallback = {
                primaryNavBarConfig?.let {
                    SystemUtil.applyNavBarConfig(mContext, mWindow, it)
                }
            }
        }

        mainSideNav?.also {
            listOf(
                    mainSideNavUpdates!! to R.id.updatesFragment,
                    mainSideNavUnits!! to R.id.unitsFragment,
                    mainSideNavMore!! to R.id.moreFragment
            ).forEach {
                it.first.setOnClickListener { _ -> mainNav(it.second) }
            }
        }

        // after this invoke, back stack will be cleared because destination gets changed right away when showing the primary page
        navController.addOnDestinationChangedListener { _, destination, _ ->
            viewModel.removeCurrentUnit()
            destinationChanged(destination.id)
            // Clear callback stack in case back pressing logic gets messed up
            clearBackPressedCallback()
            // keep backup to be restored later in onBackPressed
            // invoked when across configuration change
            if (shouldKeepBackup) {
                shouldKeepBackup = false
            } else if (backupCallback != null) {
                backupCallback = null
            }
        }

        mainTB.setOnMenuItemClickListener(this)

        val launchItemName = intent?.getStringExtra(LAUNCH_ITEM) ?: ""
        if (launchItemName.isNotEmpty()) {
            val itemArgs = intent?.getBundleExtra(LAUNCH_ITEM_ARGS)
            val hasArgs = itemArgs != null
            val itemUnitDesc = Unit.getDescription(launchItemName)
            if (itemUnitDesc != null) {
                Handler(Looper.getMainLooper()).postDelayed(100) {
                    if (hasArgs) {
                        viewModel.displayUnit(itemUnitDesc.unitName, false, true, itemArgs)
                    } else {
                        viewModel.displayUnit(itemUnitDesc.unitName, shouldExitAppAfterBack = true)
                    }
                }
            }
        }

        applyInsets()

        val viewHandler = Handler()
        runnable {
            isToolbarInflated = mainTB.width > 0 && mainTB.height > 0
            if (isToolbarInflated) {
                if (!mainApplication.dead) initExterior()
                viewHandler.removeCallbacks(this)
            } else {
                viewHandler.postDelayed(this, 150)
            }
            // todo sleep?
//        try {
//            Thread.sleep(150)
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
        }.run()
    }

    private val navScrollBehavior: MyHideBottomViewOnScrollBehavior<View>?
        get() {
            val nav = mainBottomNav ?: return null
            val params = nav.layoutParams as CoordinatorLayout.LayoutParams
            return params.behavior as MyHideBottomViewOnScrollBehavior
        }

    private fun showNav() {
        navScrollBehavior?.slideUp(mainBottomNav!!)
    }

    private fun hideNav() {
        navScrollBehavior?.slideDown(mainBottomNav!!)
    }

    private fun destinationChanged(id: Int) {
        mainBottomNav?.let { b ->
            val menu = b.menu
            val menuItem = menu.findItem(id)
            if (menuItem != null) {
                menuItem.isChecked = true
                showNav()
            } else {
//                for(i in 0 until menu.size()) {
//                    val checkable = menu[i]
//                    if (!checkable.isChecked) continue
//                    checkable.isChecked = false
//                    break
//                }
                hideNav()
            }
        }
        mainSideNav?.let { _ ->
            when (id) {
                R.id.updatesFragment -> R.id.mainSideNavUpdates
                R.id.unitsFragment -> R.id.mainSideNavUnits
                R.id.moreFragment -> R.id.mainSideNavMore
                else -> null
            }.let { vId ->
                val radioGroup = mainSideNavRadioGroup!!
                val item: MaterialRadioButton? = if (vId == null) null else radioGroup.findViewById(vId)
                if (item != null) {
                    item.isChecked = true
                } else {
                    for (i in 0 until radioGroup.childCount) {
                        val checkable = radioGroup[i] as Checkable
                        if (!checkable.isChecked) continue
                        checkable.isChecked = false
                        break
                    }
                }
            }
        }
    }

    private fun mainNav(id: Int) {
        val isAlreadyDestination = navController.currentDestination?.id == id
        if (isAlreadyDestination) return
        viewModel.removeCurrentUnit()
        val mainDestinationId = R.id.updatesFragment
        val isToMain = id == mainDestinationId
        NavOptions.Builder().run {
            setPopUpTo(mainDestinationId, isToMain)
            setLaunchSingleTop(true)
            setEnterAnim(R.anim.fragment_open_enter)
            setExitAnim(R.anim.fragment_fade_exit)
            setPopEnterAnim(R.anim.fragment_fade_enter)
            setPopExitAnim(R.anim.fragment_close_exit)
        }.also { navController.navigate(id, null, it.build()) }
    }

    private fun goAllTheWayBack() {
        val mainDestinationId = R.id.updatesFragment
        val isAlreadyDestination = navController.currentDestination?.id == mainDestinationId
        if (isAlreadyDestination) finish()
        else mainNav(mainDestinationId)
    }

    private fun applyInsets() {
        background.setOnApplyWindowInsetsListener { _, insets ->
            consumeInsets(WindowInsets(insets))
            insets
        }
    }

    private fun consumeInsets(insets: WindowInsets) {
        val app = mainApplication
        app.insetTop = insets.top
        app.insetBottom = insets.bottom
        app.insetLeft = insets.left
        app.insetRight = insets.right
        viewModel.insetTop.value = app.insetTop
        viewModel.insetBottom.value = app.insetBottom
        viewModel.insetLeft.value = app.insetLeft
        viewModel.insetRight.value = app.insetRight
    }

    private fun applyColor() {
        GlobalScope.launch {
            val colorFore: Int
            val colorBack: Int
            val isDarkStatus: Boolean
            if (mainApplication.exterior) {
                var offsetX = viewModel.insetLeft.value ?: 0
                if (isLand) offsetX += mainSideNav?.width ?: 0
                val colors = X.extractBackColor(mainTB, background, offsetX) // extremely heavy
                colorFore = colors[0]
                colorBack = colors[1]
                val backColor = colorBack and X.getColor(mContext, R.color.exteriorTransparencyColor)
                setToolbarBackColor(this, backColor)
                isDarkStatus = colorFore == Color.BLACK
                launch(Dispatchers.Main) {
                    mWindow.let {
                        SystemUtil.applyStatusBarColor(mContext, it, isDarkStatus, true)
                        val isTransparentNav = mainApplication.exterior || (viewModel.insetBottom.value ?: 0) < X.size(mContext, 15f, X.DP)
                        primaryNavBarConfig = SystemBarConfig(isDarkStatus, isTransparentBar = isTransparentNav)
                        SystemUtil.applyNavBarConfig(mContext, it, primaryNavBarConfig!!)
                        navScrollBehavior?.onSlidedUpCallback?.invoke()
                    }
                }
            } else {
                colorBack = ThemeUtil.getColor(mContext, R.attr.colorTb)
                launch(Dispatchers.Main) {
                    setToolbarBackColor(this, colorBack)
                    mWindow.also {
                        primaryNavBarConfig = SystemUtil.applyDefaultSystemUiVisibility(mContext, it, viewModel.insetBottom.value ?: 0).second
                        navScrollBehavior?.onSlidedUpCallback?.invoke()
                    }
                }
            }
        }
    }

    private fun setToolbarBackColor(scope: CoroutineScope, color: Int) {
        scope.launch {
            if (isLand) {
                val drawable = mContext.getDrawable(R.drawable.exterior_toolbar_back)
                drawable?.setTint(color)
                launch(Dispatchers.Main) {
                    mainTB.background = drawable
                }
            } else {
                launch(Dispatchers.Main) {
                    mainTB.setBackgroundColor(color)
                    mainBottomNav!!.setBackgroundColor(color)
                }
            }
        }
    }

    private fun applyExterior() {
        val app = mainApplication
        if (!app.exterior) return
        if (app.background == null) {
            background.background = null
            return
        }
        if (X.aboveOn(X.N) && isInMultiWindowMode) X.setSplitBackground(mContext, background, X.getCurrentAppResolution(mContext))
        else X.setBackground(mContext, background)
    }

    private fun initExterior() {
//        if (viewModel.background.value == mainApplication.background) return
        if (!isToolbarInflated) return
        viewModel.background.value = mainApplication.background
    }

    /**
     * for app background changing
     */
    private fun updateExterior() {
        mWindow.let {
            primaryNavBarConfig = SystemBarConfig(mainApplication.isPaleTheme, isTransparentBar = mainApplication.exterior)
            SystemUtil.applyNavBarConfig(mContext, it, primaryNavBarConfig!!)
        }
        if (!mainApplication.exterior) background.background = null
        initExterior()
    }
}
