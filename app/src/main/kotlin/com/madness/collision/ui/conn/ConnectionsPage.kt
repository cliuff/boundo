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

package com.madness.collision.ui.conn

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.compose.AndroidFragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.madness.collision.R
import com.madness.collision.chief.app.asInsets
import com.madness.collision.chief.layout.share
import com.madness.collision.main.showPage
import com.madness.collision.settings.DeviceControlsFragment
import com.madness.collision.settings.instant.InstantFragment
import com.madness.collision.ui.comp.MetaSurface
import com.madness.collision.ui.theme.MetaAppTheme
import com.madness.collision.ui.theme.PreviewAppTheme
import com.madness.collision.unit.device_manager.list.DeviceListController
import com.madness.collision.unit.device_manager.list.DeviceListUiState
import com.madness.collision.util.config.LocaleUtils
import com.madness.collision.util.dev.PreviewCombinedColorLayout
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

@Immutable
data class ConnUiState(
    val message: ConnPermMessage?,
)

@Immutable
class ConnPermMessage(
    val text: String,
    val action: String,
    val onClick: (DeviceListController) -> Unit,
)

internal fun DeviceListUiState.toMessage(): ConnPermMessage? =
    when (this) {
        DeviceListUiState.BluetoothDisabled ->
            ConnPermMessage("Bluetooth OFF", "Turn on", DeviceListController::requestBluetoothOn)
        DeviceListUiState.PermissionDenied ->
            ConnPermMessage("BLUETOOTH_CONNECT permission denied", "Allow", DeviceListController::requestBluetoothConn)
        DeviceListUiState.PermissionPermanentlyDenied ->
            ConnPermMessage("BLUETOOTH_CONNECT permission should be granted from app settings", "Change", DeviceListController::requestSettingsChange)
        else -> null
    }

interface ConnEventHandler {
    fun showAppSettings()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionsPage(eventHandler: ConnEventHandler, contentPadding: PaddingValues) {
    val viewModel = viewModel<ConnectionsViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ConnectionsAppBar(
                title = {
                    Text(
                        text = "Connections"
                            .run { remember { lowercase(LocaleUtils.getRuntimeFirst()) } },
                        fontWeight = FontWeight.Medium,
                    )
                },
                onClickSettings = eventHandler::showAppSettings,
                windowInsets = contentPadding.asInsets()
                    .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                    .share(WindowInsets(top = 5.dp)),
                scrollBehavior = scrollBehavior,
            )
        },
        containerColor = MetaAppTheme.colorScheme.backgroundNeutral,
        contentWindowInsets = contentPadding.asInsets(),
    ) { innerPadding ->
        ConnectionsContent(
            modifier = Modifier.fillMaxSize(),
            message = uiState.message,
            contentPadding = innerPadding,
        )
    }
}

@Composable
private fun ConnectionsContent(
    message: ConnPermMessage?,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues.Zero,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        MetaSurface(
            shape = AbsoluteSmoothCornerShape(20.dp, 80),
            content = { DeviceList(message = message) },
        )
        MetaSurface(
            shape = AbsoluteSmoothCornerShape(20.dp, 80),
            content = { SystemUtilities() },
        )
        val context = LocalContext.current
        MetaSurface(
            onClick = { context.showPage<InstantFragment>() },
            shape = AbsoluteSmoothCornerShape(20.dp, 80),
            content = { AppShortcutsEntry() },
        )
    }
}

@Composable
private fun DeviceList(
    message: ConnPermMessage?,
    modifier: Modifier = Modifier,
) {
    var controller: DeviceListController? by remember { mutableStateOf(null) }

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 20.dp)) {
        Text(
            modifier = Modifier.padding(start = 20.dp, end = 5.dp),
            text = stringResource(R.string.dm_main_paired_devices),
            style = MaterialTheme.typography.headlineSmall,
        )
        if (message != null) {
            PermissionMessage(
                modifier = Modifier.padding(horizontal = 20.dp),
                text = message.text,
                action = message.action,
                onClick = { controller?.let(message.onClick) },
            )
        }
        AndroidFragment<ConnDevicesCommWrapper>(
            modifier = Modifier.fillMaxWidth().animateContentSize(),
            onUpdate = { controller = it.getDeviceListFragment()?.getController() }
        )

        val context = LocalContext.current
        DeviceControlsEntry(
            modifier = Modifier
                .padding(top = 10.dp)
                .padding(horizontal = 20.dp),
            onClick = { context.showPage<DeviceControlsFragment>() },
        )
    }
}

@Composable
private fun PermissionMessage(
    text: String,
    action: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            modifier = Modifier.weight(1f),
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            overflow = TextOverflow.Ellipsis,
            maxLines = 2,
        )
        Spacer(modifier = Modifier.width(10.dp))
        FilledTonalButton(
            modifier = Modifier.heightIn(min = 32.dp),
            onClick = onClick,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 5.dp),
        ) {
            Text(
                modifier = Modifier.widthIn(max = 120.dp),
                text = action,
                style = MaterialTheme.typography.labelMedium,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }
    }
}

@Composable
@PreviewCombinedColorLayout
private fun ConnectionsPreview() {
    PreviewAppTheme {
        Surface(modifier = Modifier.fillMaxWidth(), color = MetaAppTheme.colorScheme.backgroundNeutral) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val message = ConnPermMessage("Bluetooth OFF", "Turn ooooooooooon", {})
                MetaSurface(
                    shape = AbsoluteSmoothCornerShape(20.dp, 80),
                    content = { DeviceList(message = message) },
                )

                val message1 = ConnPermMessage("Bluetooth OFF", "开启", {})
                MetaSurface(
                    shape = AbsoluteSmoothCornerShape(20.dp, 80),
                    content = { DeviceList(message = message1) },
                )
                MetaSurface(
                    shape = AbsoluteSmoothCornerShape(20.dp, 80),
                    content = { AppShortcutsEntry() },
                )
            }
        }
    }
}
