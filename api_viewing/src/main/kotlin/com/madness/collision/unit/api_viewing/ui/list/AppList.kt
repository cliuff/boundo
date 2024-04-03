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

package com.madness.collision.unit.api_viewing.ui.list

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.isVisible
import androidx.lifecycle.viewmodel.compose.viewModel
import com.madness.collision.Democratic
import com.madness.collision.chief.app.ComposeFragment
import com.madness.collision.chief.app.rememberColorScheme
import com.madness.collision.main.MainViewModel
import com.madness.collision.unit.api_viewing.databinding.AvLegacyAppListBinding
import com.madness.collision.unit.api_viewing.list.AppListFragment
import kotlin.math.roundToInt

class AppListFragment : ComposeFragment(), Democratic {
    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
//        mainViewModel.configNavigation(toolbar, iconColor)
//        toolbar.setTitle(com.madness.collision.R.string.apiViewer)
        toolbar.isVisible = false
        return true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        democratize(mainViewModel)
    }

    @Composable
    override fun ComposeContent() {
        MaterialTheme(colorScheme = rememberColorScheme()) {
            AppList(paddingValues = rememberContentPadding())
        }
    }
}

@Composable
fun AppList(paddingValues: PaddingValues) {
    AppListLegacy(paddingValues = paddingValues)
}

@Composable
private fun AppListV2(paddingValues: PaddingValues) {
    val viewModel = viewModel<AppListViewModel>()
    val artList by viewModel.uiState.collectAsState()
    Box(modifier = Modifier.background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))) {
        LazyColumn(contentPadding = paddingValues) {
            items(items = artList, key = { p -> p.packageName }) { art ->
                AppListItem(art = art)
            }
        }
    }
}

@Composable
private fun AppListLegacy(paddingValues: PaddingValues) {
    val mainViewModel = viewModel<MainViewModel>()
    val viewModel = viewModel<AppListViewModel>()
    val appList by viewModel.appList.collectAsState()
    val opUiState by viewModel.opUiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) { viewModel.init(context, lifecycleOwner, mainViewModel.timestamp) }

    val insetTop = with(LocalDensity.current) { paddingValues.calculateTopPadding().toPx().roundToInt() }
    Box(modifier = Modifier.background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))) {
        var listFragment: AppListFragment? by remember { mutableStateOf(null) }
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val con = AvLegacyAppListBinding.inflate(LayoutInflater.from(ctx)).root
                con.also { listFragment = con.getFragment() }
            },
            update = { fContainer ->
                listFragment?.getAdapter()?.topCover = insetTop
                listFragment?.getAdapter()?.setSortMethod(opUiState.options.listOrder.code)
                listFragment?.updateList(appList)
                requireNotNull(listFragment)
            },
        )

        var showListOptions by remember { mutableIntStateOf(0) }
        var showTagFilter by remember { mutableIntStateOf(0) }
        AppListBar(paddingValues = PaddingValues(top = 28.dp)) {
            AppBarTool(icon = Icons.Outlined.CheckCircle, label = "Options", onClick = { showListOptions++ })
//            Spacer(modifier = Modifier.width(4.dp))
            AppBarTool(icon = Icons.Outlined.FilterAlt, label = "Filter", onClick = { showTagFilter++ })
            AppBarTool(icon = Icons.Outlined.PieChart, label = "Stats", onClick = {  })
            AppBarTool(icon = Icons.Outlined.MoreVert, label = "", onClick = {  })
        }

        ListOptionsDialog(
            isShown = showListOptions,
            options = opUiState.options,
            viewModel = viewModel,
        )
        TagFilterDialog(isShown = showTagFilter, viewModel = viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListOptionsDialog(isShown: Int, options: AppListOptions, viewModel: AppListViewModel) {
    val conLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) {
        viewModel.toggleListSrc(AppListSrc.SelectApks(it))
    }
    val volLauncher = rememberLauncherForActivityResult(OpenVolumeContract()) {
        viewModel.toggleListSrc(AppListSrc.SelectVolume(it))
    }
    var showBottomSheet by remember(isShown) { mutableStateOf(isShown > 0) }
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            AppListOptions(
                options = options,
                onClickSrc = viewModel::toggleListSrc,
                onSelectApks = {
                    when (viewModel.containsListSrc(AppListSrc.SelectApks)) {
                        true -> viewModel.removeListSrc(AppListSrc.SelectApks)
                        else -> conLauncher.launch("application/vnd.android.package-archive")
                    }
                },
                onSelectFolder = {
                    when (viewModel.containsListSrc(AppListSrc.SelectVolume)) {
                        true -> viewModel.removeListSrc(AppListSrc.SelectVolume)
                        else -> volLauncher.launch(null)
                    }
                },
                onClickOrder = viewModel::setListOrder,
                onClickApiMode = viewModel::setApiMode,
            )
        }
    }
}

private class OpenVolumeContract : ActivityResultContracts.OpenDocumentTree() {
    override fun createIntent(context: Context, input: Uri?): Intent {
        return super.createIntent(context, input).apply {
            // make it possible to access children
            addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagFilterDialog(isShown: Int, viewModel: AppListViewModel) {
    var showBottomSheet by remember(isShown) { mutableStateOf(isShown > 0) }
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            val tagState = remember { ListTagState() }
            AppListTags(tagState = tagState)
        }
    }
}
