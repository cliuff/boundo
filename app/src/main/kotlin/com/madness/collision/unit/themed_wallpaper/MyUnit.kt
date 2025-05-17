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

package com.madness.collision.unit.themed_wallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.MarginLayoutParams
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import coil3.load
import com.madness.collision.BuildConfig
import com.madness.collision.R
import com.madness.collision.chief.auth.AppOpsMaster
import com.madness.collision.chief.auth.PermissionState
import com.madness.collision.databinding.UnitThemedWallpaperBinding
import com.madness.collision.settings.ExteriorFragment
import com.madness.collision.unit.Unit
import com.madness.collision.util.F
import com.madness.collision.util.alterPadding
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MyUnit: Unit() {

    override val id: String = "TW"

    private val mutPermState: MutableStateFlow<PermissionState> =
        MutableStateFlow(PermissionState.Granted)
    private val permState: StateFlow<PermissionState> by ::mutPermState
    private var previewLoadTimestamp: Long = 0L
    private lateinit var viewBinding: UnitThemedWallpaperBinding

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        configNavigation(toolbar, iconColor)
        toolbar.setTitle(R.string.twService)
        inflateAndTint(R.menu.toolbar_tw, toolbar, iconColor)
        return true
    }

    override fun selectOption(item: MenuItem): Boolean {
        when (item.itemId){
            R.id.twToolbarDone -> {
                val context = context ?: return false
                val comp = ComponentName(context, ThemedWallpaperService::class.java)
                val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                    .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, comp)
                startActivity(intent)
                return true
            }
        }
        return false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = UnitThemedWallpaperBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        democratize()
        viewBinding.twImgLight.setOnClickListener {
            ExteriorFragment.newInstance(ExteriorFragment.MODE_TW_LIGHT)
                .let { mainViewModel.displayFragment(it) }
        }
        viewBinding.twImgDark.setOnClickListener {
            ExteriorFragment.newInstance(ExteriorFragment.MODE_TW_DARK)
                .let { mainViewModel.displayFragment(it) }
        }
        mainViewModel.contentWidthTop.observe(viewLifecycleOwner){
            viewBinding.twRoot.alterPadding(top = it)
            viewBinding.twMessageContainer.updateLayoutParams<MarginLayoutParams> { topMargin = it }
        }
        permState
            .flowWithLifecycle(viewLifecycleOwner.lifecycle)
            .onEach(::resolvePermState)
            .launchIn(viewLifecycleOwner.lifecycleScope)
        val context = context ?: return
        loadPreview(context)
    }

    override fun onResume() {
        super.onResume()
        val state = when {
            AppOpsMaster.isDynamicWallpaperAllowed() -> PermissionState.Granted
            else -> PermissionState.Denied(0)
        }
        mutPermState.update { state }
        if (lifecycleEventTime.compareValues(Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_CREATE) > 0) {
            if (MyBridge.changeTimestamp > previewLoadTimestamp) context?.let(::loadPreview)
        }
    }

    private fun resolvePermState(state: PermissionState) {
        if (state.isGranted) {
            viewBinding.twMessageContainer.isInvisible = true
        } else {
            setMessageAndAction("[MIUI] Dynamic wallpaper permission should be granted from app settings", "Change")
            viewBinding.twAction.setOnClickListener { requestSettingsChange() }
        }
    }

    private fun setMessageAndAction(text: String, action: String) = viewBinding.run {
        twMessage.text = text
        twAction.text = action
        twMessage.isVisible = true
        twAction.isVisible = true
        twMessageContainer.isVisible = true
    }

    private val appSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { }

    private fun requestSettingsChange() {
        val uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
        // as stated in the doc, avoid using Intent.FLAG_ACTIVITY_NEW_TASK with startActivityForResult()
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri)
        appSettingsLauncher.launch(intent)
    }

    private fun loadPreview(context: Context) {
        lifecycleScope.launch {
            previewLoadTimestamp = System.currentTimeMillis()
            val views = listOf(viewBinding.twImgLight, viewBinding.twImgDark)
            loadWallpapers(context).zip(views).forEach { (imgAny, view) ->
                // width is defined as 150dp in XML, change height to properly show ColorDrawable
                val vh = if (imgAny is Int) view.width else LayoutParams.WRAP_CONTENT
                view.updateLayoutParams<LayoutParams> { height = vh }
                view.load(if (imgAny is Int) ColorDrawable(imgAny) else imgAny)
            }
        }
    }

}

/** @return [File] | [ColorInt][Int] */
private suspend fun loadWallpapers(context: Context): List<Any?> {
    val handler = CoroutineExceptionHandler { _, t -> t.printStackTrace() }
    return withContext(Dispatchers.IO + handler) {
        val paths = listOf(F.valFilePubTwPortrait(context), F.valFilePubTwPortraitDark(context))
        val files = paths.map { path -> File(path).takeIf { it.exists() && it.canRead() } }
        buildList(2) {
            addAll(files)
            if (get(0) == null) set(0, Color.WHITE)
            if (get(1) == null) set(1, Color.BLACK)
        }
    }
}
