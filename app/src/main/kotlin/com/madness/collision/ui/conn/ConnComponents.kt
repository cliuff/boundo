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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commitNow
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.compose.viewModel
import com.madness.collision.R
import com.madness.collision.main.UnitBarPage
import com.madness.collision.ui.comp.ClassicTopAppBarDefaults
import com.madness.collision.ui.theme.MetaAppTheme
import com.madness.collision.unit.device_manager.list.DeviceListFragment
import com.madness.collision.unit.device_manager.list.DeviceListUiState
import kotlin.getValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConnectionsAppBar(
    title: @Composable () -> Unit,
    onClickSettings: () -> Unit,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    val styledTitle = @Composable {
        ProvideTextStyle(TextStyle(fontFamily = ClassicTopAppBarDefaults.FontFamily), title)
    }
    CenterAlignedTopAppBar(
        title = styledTitle,
        actions = {
            IconButton(onClick = onClickSettings) {
                Icon(
                    modifier = Modifier.size(22.dp),
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = null,
                )
            }
            val endPadding = windowInsets.asPaddingValues()
                .calculateEndPadding(LocalLayoutDirection.current)
                .let { if (it >= 5.dp) 2.dp else 5.dp }
            Spacer(modifier = Modifier.width(endPadding))
        },
        windowInsets = windowInsets,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MetaAppTheme.colorScheme.backgroundNeutral,
            scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = .96f)),
        scrollBehavior = scrollBehavior,
    )
}

@Composable
internal fun DeviceControlsEntry(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.conn_dev_controls),
                    style = MaterialTheme.typography.bodyMedium,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
                Text(
                    text = stringResource(R.string.conn_dev_controls_desc),
                    style = MaterialTheme.typography.bodySmall,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
            )
        }
    }
}

@Composable
internal fun AppShortcutsEntry(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(R.string.Main_TextView_Launcher),
            style = MaterialTheme.typography.bodyLarge,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
        )
    }
}

@Composable
internal fun SystemUtilities(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier) {
        // retrieve view model from host activity
        val activity = LocalActivity.current as? ComponentActivity
        UnitBarPage(
            modifier = Modifier.padding(vertical = 12.dp),
            mainViewModel = viewModel(activity!!),
            width = maxWidth,
        )
    }
}


private val ConnDevNavContainerId: Int = View.generateViewId()

internal class ConnDevicesCommWrapper : Fragment(), DeviceListFragment.Listener {
    private val viewModel: ConnectionsViewModel by viewModels({ parentFragment ?: this })

    override fun onUiState(state: DeviceListUiState) {
        viewModel.setPermMessage(state.toMessage())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentContainerView(inflater.context).apply { id = ConnDevNavContainerId }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            val devFragment = childFragmentManager.findFragmentByTag("Dev")
            childFragmentManager.commitNow {
                if (devFragment?.isAdded == true) {
                    remove(devFragment)
                    add(ConnDevNavContainerId, devFragment, "Dev")
                } else {
                    add(ConnDevNavContainerId, DeviceListFragment::class.java, null, "Dev")
                }
                setReorderingAllowed(true)
            }
        }
    }

    fun getDeviceListFragment(): DeviceListFragment? =
        childFragmentManager.findFragmentByTag("Dev") as? DeviceListFragment
}
