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

package com.madness.collision.unit.api_viewing.list

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.madness.collision.Democratic
import com.madness.collision.R
import com.madness.collision.main.MainViewModel
import com.madness.collision.misc.MiscApp
import com.madness.collision.unit.api_viewing.data.AppIcon
import com.madness.collision.unit.api_viewing.util.ApkUtil
import com.madness.collision.unit.api_viewing.util.ManifestUtil
import com.madness.collision.util.*
import com.madness.collision.util.os.OsUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import com.madness.collision.unit.api_viewing.R as MyR

internal class AppIconFragment : TaggedFragment(), Democratic {

    override val category: String = "AV"
    override val id: String = "AppIcon"

    companion object {
        const val ARG_APP_NAME = "Name"
        const val ARG_PACKAGE_NAME = "PackageName"
        const val ARG_APK_Path = "ApkPath"
        const val ARG_IS_ARCHIVE = "IsArchive"

        @JvmStatic
        fun newInstance(appName: String, packageName: String, path: String, isArchive: Boolean) = AppIconFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_APP_NAME, appName)
                putString(ARG_PACKAGE_NAME, packageName)
                putString(ARG_APK_Path, path)
                putBoolean(ARG_IS_ARCHIVE, isArchive)
            }
        }
    }

    private val mainViewModel: MainViewModel by activityViewModels()
    private val viewModel: AppIconViewModel by viewModels()
    private var mutableComposeView: ComposeView? = null
    private val composeView: ComposeView get() = mutableComposeView!!

    private var apkRes: Resources? = null
    private var apkPath: String? = null

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        mainViewModel.configNavigation(toolbar, iconColor)
        toolbar.title = arguments?.getString(ARG_APP_NAME) ?: ""
        inflateAndTint(MyR.menu.toolbar_av_icon, toolbar, iconColor)
        return true
    }

    override fun selectOption(item: MenuItem): Boolean {
        when (item.itemId) {
            MyR.id.avIconToolbarTheme -> {
                viewModel.darkUiState.update { !it }
                return true
            }
            MyR.id.avIconToolbarManual -> {
                val context = context ?: return false
                CollisionDialog.alert(context, MyR.string.apiInfoAiOverallDes).show()
                return true
            }
        }
        return false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        mutableComposeView = ComposeView(inflater.context)
        return composeView
    }

    override fun onDestroyView() {
        mutableComposeView = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val arguments = arguments ?: return
        val appName = arguments.getString(ARG_APP_NAME) ?: ""
        val packageName = arguments.getString(ARG_PACKAGE_NAME) ?: ""
        val apkPath = arguments.getString(ARG_APK_Path) ?: ""
        val isArchive = arguments.getBoolean(ARG_IS_ARCHIVE)

        this.apkPath = apkPath

        democratize(mainViewModel)
        val context = context ?: return
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        val env = AppIconEnv(appName)
        val compViewModel = viewModel
        composeView.setContent {
            val isDark by compViewModel.darkUiState.collectAsState()
            val colorScheme = remember(isDark) {
                if (OsUtils.satisfy(OsUtils.S)) {
                    if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                } else {
                    if (isDark) darkColorScheme() else lightColorScheme()
                }
            }
            MaterialTheme(colorScheme = colorScheme) {
                AppIconPage(mainViewModel, env)
            }
        }

        val viewLifecycle = viewLifecycleOwner.lifecycle
        viewModel.eventState
            .flowWithLifecycle(viewLifecycle)
            .onEach { handleEvent(it); viewModel.triggerEvent(AppIconEvent.None) }
            .launchIn(viewLifecycle.coroutineScope + Dispatchers.Default)
        viewModel.darkUiState
            .flowWithLifecycle(viewLifecycle)
            .distinctUntilChanged()  // distinct
            .onEach dark@{ isDark ->
                val decor = activity?.window?.decorView ?: return@dark
                val color = if (isDark == mainApplication.isDarkTheme) {
                    ThemeUtil.getColor(context, R.attr.colorABackground)
                } else {
                    val colorRes = if (isDark) R.color.backgroundABlack else R.color.backgroundAWhite
                    ContextCompat.getColor(context, colorRes)
                }
                decor.setBackgroundColor(color)
            }
            .launchIn(viewLifecycle.coroutineScope + Dispatchers.Main)

        lifecycleScope.launch(Dispatchers.Default) {
            viewModel.state = IconLoadingState.Loading to IconLoadingState.Loading
            val appInfo = if (isArchive) MiscApp.getApplicationInfo(context, apkPath = apkPath)
            else MiscApp.getApplicationInfo(context, packageName = packageName)
            appInfo ?: return@launch

            val apkFile = File(if (isArchive) apkPath else appInfo.sourceDir)
            if (apkFile.exists().not() || apkFile.canRead().not()) return@launch
            val pkgMan = context.packageManager
            val res = try {
                pkgMan.getResourcesForApplication(appInfo)
            } catch (e: Exception) {
                e.printStackTrace()
                return@launch
            }
            apkRes = res
            val iconJobs = listOf(
                async {
                    val id = appInfo.icon.takeIf { it != 0 }
                    val name = getString(MyR.string.av_ic_info_label_runtime)
                    val item = IconInfo.Item(name, null, IconInfo.Source.API)
                    val info = IconInfo(id, appInfo.loadIcon(pkgMan), null, IconInfo.MonoEntry(item))
                    listOf(info).mapResEntry(res)
                },
                async { getIconSet(apkPath, res).mapResEntry(res) },
            )
            val launcherJob = async ic@{
                if (isArchive) return@ic emptyList()
                val set = getLauncherIconSet(pkgMan, packageName, appInfo)
                // eliminate icon duplicates with normal and round APK icons
                val definedSet = iconJobs[1].await().mapNotNullTo(HashSet()) { it?.resId }
                set.filterNot { it.resId in definedSet }.mapResEntry(res)
            }
            val iconJobIcons = iconJobs.awaitAll().flatten().filterNotNull().initPreviews(context)
            val appState = IconLoadingState.Result(iconJobIcons)
            viewModel.state = appState to IconLoadingState.Loading
            val launcherJobIcons = launcherJob.await().filterNotNull().initPreviews(context)
            val launcherState = IconLoadingState.Result(launcherJobIcons)
            viewModel.state = appState to launcherState
        }
    }

    private fun List<IconInfo?>.mapResEntry(res: Resources) = map {
        it ?: return@map null
        val id = it.resId ?: return@map it
        val (type, entry) = ApkUtil.getResourceEntryName(res, id)
        IconInfo(it.resId, it.icon, "R.$type.$entry", it.entry)
    }

    private fun List<IconInfo>.initPreviews(context: Context) = onEach {
        val icon = it.icon as? IconInfo.AdaptiveIcon ?: return@onEach
        icon.previews = AppIcon(context, icon)
    }

    private fun handleEvent(event: AppIconEvent) = when (event) {
        AppIconEvent.None -> Unit
        is AppIconEvent.ShareIcon -> run action@{
            val context = context ?: return@action
            actionIcon(context, event.drawable, event.exportName)
        }
        is AppIconEvent.ShowIconRes -> run action@{
            val context = context ?: return@action
            val res = apkRes ?: return@action
            val path = apkPath ?: return@action
            actionIconRes(context, res, event.iconId, path)
        }
    }

    private fun getIconSet(sourceDir: String, res: Resources): List<IconInfo> {
        val nameList = listOf(MyR.string.av_ic_info_label_apk_normal,
            MyR.string.av_ic_info_label_apk_round).map { getString(it) }
        val apkIconAttr = arrayOf(arrayOf("application", "icon"), arrayOf("application", "roundIcon"))
        val idList = try {
            ManifestUtil.mapAttrs(sourceDir, apkIconAttr) icon@{ index, value ->
                if (value.isNullOrBlank()) return@icon null
                val id = value.toIntOrNull() ?: return@icon null
                index to id
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
        idList ?: return emptyList()
        val mergedList = idList.filterNotNull().groupBy { it.second }.mapNotNull merge@{ (id, list) ->
            if (id == 0 || list.isEmpty()) return@merge null
            id to list.map { IconInfo.Item(nameList[it.first], null, IconInfo.Source.APK) }
        }
        return mergedList.mapNotNull info@{ (id, items) ->
            try {
                val drawable = ResourcesCompat.getDrawable(res, id, null) ?: return@info null
                val entry = if (items.size == 1) IconInfo.MonoEntry(items[0]) else IconInfo.PolyEntry(items)
                IconInfo(id, drawable, null, entry)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun getLauncherIconSet(pkgMan: PackageManager, pkgName: String, appInfo: ApplicationInfo): List<IconInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val flag = if (OsUtils.satisfy(OsUtils.N)) PackageManager.MATCH_DISABLED_COMPONENTS
        else flagGetDisabledLegacy
        val resolveList = if (OsUtils.satisfy(OsUtils.T)) {
            val flags = PackageManager.ResolveInfoFlags.of(flag.toLong())
            pkgMan.queryIntentActivities(intent, flags)
        } else {
            pkgMan.queryLegacy(intent, flag)
        }
        val idList = resolveList.asSequence()
            .filter { it.activityInfo.packageName == pkgName }
            // filter out activity-alias only, which is used to implement alternate app icon
            // this will exclude launcher activities of normal shortcuts
//            .filter { it.activityInfo.targetActivity != null }
            .groupBy { it.iconResource }
        // merge the desc (activity name) of items having the same icon ID
        val mergedList = idList.mapNotNull merge@{ (icId, list) ->
            if (icId == 0 || list.isEmpty()) return@merge null
            icId to list.mapNotNull { it.activityInfo.name }.map { qualified ->
                val index = qualified.lastIndexOf('.')
                val simplified = if (index >= 0) qualified.substring(index + 1) else qualified
                IconInfo.Item(simplified, qualified.takeIf { it != simplified }, IconInfo.Source.API)
            }
        }
        val result = mergedList.mapNotNull info@{ (icId, items) ->
            val drawable = pkgMan.getDrawable(pkgName, icId, appInfo) ?: return@info null
            val entry = if (items.size == 1) IconInfo.MonoEntry(items[0]) else IconInfo.PolyEntry(items)
            IconInfo(icId, drawable, null, entry)
        }
        return result
    }

    @Suppress("deprecation")
    private fun PackageManager.queryLegacy(intent: Intent, flags: Int) =
        queryIntentActivities(intent, flags)

    @Suppress("deprecation")
    private val flagGetDisabledLegacy = PackageManager.GET_DISABLED_COMPONENTS

    private fun actionIconRes(context: Context, res: Resources, iconId: Int, apkPath: String) {
        if (iconId == 0) return
        lifecycleScope.launch(Dispatchers.Default) {
            val (title, entries) = ApkUtil.getResourceEntries(res, iconId, apkPath)
            val content = kotlin.run content@{
                if (title.isEmpty()) return@content null
                if (entries.isEmpty()) return@content title
                entries.joinToString("\n", prefix = "$title\n\n")
            }
            withContext(Dispatchers.Main) {
                if (content == null) CollisionDialog.alert(context, R.string.text_no_content).show()
                else CollisionDialog.alert(context, content).show()
            }
        }
    }

    private fun actionIcon(context: Context, imgIcon: Drawable, exportName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val bitmap = GraphicsUtil.convertDrawableToBitmap(imgIcon)
            actionIcon(context, bitmap, exportName)
        }
    }

    private fun actionIcon(context: Context, imgIcon: Bitmap, exportName: String){
        lifecycleScope.launch(Dispatchers.IO) {
            val path = F.createPath(F.cachePublicPath(context), "App", "Logo", "${exportName}.png")
            val image = File(path)
            if (F.prepare4(image)) X.savePNG(imgIcon, path)
            val uri = image.getProviderUri(context)
            val pop = FilePop.by(context, uri, "image/png", R.string.textShareImage, uri, exportName)
            withContext(Dispatchers.Main) {
                pop.show(childFragmentManager, FilePop.TAG)
            }
        }
    }

}

/**
 * [resId] cannot be zero (use null instead)
 * For services that have no app icon (id is 0), system default icon is loaded
 */
data class IconInfo(val resId: Int?, val icon: Icon, val resName: String?, val entry: Entry) {
    constructor(resId: Int?, drawable: Drawable, resName: String?, entry: Entry)
            : this(resId, drawable.toIcon(), resName, entry)

    enum class Source { APK, API }
    class Item(val name: String, val fullName: String?, val source: Source)

    sealed interface Entry {
        val first: Item
        val all: List<Item>
    }
    class MonoEntry(val item: Item) : Entry {
        override val first: Item = item
        override val all: List<Item> = listOf(item)
    }
    class PolyEntry(val items: List<Item>) : Entry {
        override val first: Item = items.first()
        override val all: List<Item> = items
    }

    sealed interface Icon
    class NormalIcon(val drawable: Drawable) : Icon
    class AdaptiveIcon(
        val drawable: Drawable,
        val background: Drawable?,
        val foreground: Drawable?,
        val monochrome: Drawable?,
    ) : Icon {
        lateinit var previews: AppIcon
    }
}

private fun Drawable.toIcon() : IconInfo.Icon {
    // Adaptive icons for Android 8+
    return if (OsUtils.satisfy(OsUtils.O) && this is AdaptiveIconDrawable) {
        // Monochrome icon for Android 13+
        val mono = if(OsUtils.satisfy(OsUtils.T)) monochrome else null
        IconInfo.AdaptiveIcon(this, background, foreground, mono)
    } else {
        IconInfo.NormalIcon(this)
    }
}

sealed interface IconLoadingState<out T> {
    object None : IconLoadingState<Nothing>
    object Loading : IconLoadingState<Nothing>
    class Result<T>(val value: T) : IconLoadingState<T>
}

typealias AppIconSection = IconLoadingState<List<IconInfo>>
typealias AppIconState = Pair<AppIconSection, AppIconSection>

sealed interface AppIconEvent {
    object None : AppIconEvent
    class ShareIcon(val drawable: Drawable, val exportName: String) : AppIconEvent
    class ShowIconRes(val iconId: Int) : AppIconEvent
}

class AppIconViewModel : ViewModel() {
    private val mutableUiState: MutableStateFlow<AppIconState> =
        MutableStateFlow(IconLoadingState.None to IconLoadingState.None)
    val uiState: StateFlow<AppIconState> by ::mutableUiState
    private val mutableEventState: MutableStateFlow<AppIconEvent> = MutableStateFlow(AppIconEvent.None)
    val eventState: StateFlow<AppIconEvent> by ::mutableEventState
    val darkUiState: MutableStateFlow<Boolean> = MutableStateFlow(mainApplication.isDarkTheme)

    var state: AppIconState
        get() = uiState.value
        set(value) = updateState(value)

    fun updateState(state: AppIconState) {
        mutableUiState.update { state }
    }

    fun triggerEvent(event: AppIconEvent) {
        mutableEventState.update { event }
    }
}
