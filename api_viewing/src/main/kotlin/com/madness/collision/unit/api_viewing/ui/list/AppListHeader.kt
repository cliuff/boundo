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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.twotone.Android
import androidx.compose.material.icons.twotone.PieChart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madness.collision.chief.app.BoundoTheme
import com.madness.collision.unit.api_viewing.R
import com.madness.collision.util.dev.PreviewCombinedColorLayout
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

@Stable
interface ListHeaderState {
    val devInfoLabel: String
    val devInfoDesc: String
    var statsSize: Int
    fun setTerminalCat(cat: ListSrcCat)
    fun clearExtraCats()
    fun showStats(options: AppListOptions)
    fun showSystemModules()
    fun onQueryChange(query: String)
}

@Composable
fun AppListSwitchHeader(
    modifier: Modifier = Modifier,
    options: AppListOptions,
    appSrcState: AppSrcState,
    headerState: ListHeaderState,
) {
    Column(modifier = modifier) {
        AppListHeader(appSrcState.isLoadingSrc, options, headerState)
        AppListSwitcher(appSrcState, headerState::setTerminalCat, headerState::clearExtraCats)
    }
}

@Composable
fun AppListHeader(
    isLoadingSrc: Boolean,
    options: AppListOptions,
    headerState: ListHeaderState,
    modifier: Modifier = Modifier,
) {
    var isQuerying: Boolean by remember { mutableStateOf(false) }
    AppListHeader(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        devInfoLabel = headerState.devInfoLabel,
        devInfoDesc = headerState.devInfoDesc,
        statsSizeLabel = headerState.statsSize.takeIf { it > 0 }?.toString().orEmpty(),
        onClickDevInfo = headerState::showSystemModules,
        onClickStats = { headerState.showStats(options) },
        queryEnabled = !isLoadingSrc || isQuerying,
        onQueryChange = { q -> isQuerying = true; headerState.onQueryChange(q) },
    )
}

@Composable
fun AppListSwitcher(
    appSrcState: AppSrcState,
    onSelect: (ListSrcCat) -> Unit,
    onClear: () -> Unit,
    overrideLabel: Map<ListSrcCat, String> = emptyMap(),
) {
    val loadedSrc = appSrcState.loadedCats
    if (loadedSrc.isNotEmpty() && loadedSrc.singleOrNull() != ListSrcCat.Platform) {
        AppSrcTypeSwitcher(
            types = loadedSrc.associateWith { cat ->
                overrideLabel[cat] ?: when (cat) {
                    ListSrcCat.Platform -> stringResource(R.string.av_main_cat_platform)
                    ListSrcCat.Storage -> stringResource(R.string.av_main_cat_storage)
                    ListSrcCat.Temporary -> stringResource(R.string.av_main_cat_temporary)
                    ListSrcCat.Filter -> stringResource(R.string.av_main_cat_filter)
                }
            },
            selType = appSrcState.terminalCat,
            onSelType = onSelect,
            onClear = onClear,
        )
    }
}

@Composable
fun AppListHeader(
    modifier: Modifier = Modifier,
    devInfoLabel: String,
    devInfoDesc: String,
    statsSizeLabel: String,
    onClickDevInfo: () -> Unit,
    onClickStats: () -> Unit,
    queryEnabled: Boolean,
    onQueryChange: (String) -> Unit,
    headerType: Int = 0,
) {
    Column(modifier = modifier) {
        if (headerType == 0) {
            Row() {
                TextLabelAction(
                    modifier = Modifier.weight(2f),
                    icon = Icons.TwoTone.Android,
                    titleLabel = devInfoLabel,
                    valueLabel = "",
                    onClick = onClickDevInfo,
                )
                Spacer(modifier = Modifier.weight(1f))
                TextLabelAction(
                    modifier = Modifier.weight(2f),
                    icon = Icons.TwoTone.PieChart,
                    titleLabel = stringResource(R.string.av_main_stats),
                    valueLabel = statsSizeLabel,
                    onClick = onClickStats
                )
            }
        }

        var query by remember { mutableStateOf("") }
        val keyboardController = LocalSoftwareKeyboardController.current
        val focusManager = LocalFocusManager.current
        Spacer(modifier = Modifier.height(8.dp))
        // clear the query when navigating back
        // fixme this gets overridden by fragment back stack, results in a wrong back order
        BackHandler(enabled = query.isNotEmpty()) {
            query = ""
            onQueryChange("")
            // the keyboard has collapsed before user can go back, we only clear the focus
            focusManager.clearFocus()
        }
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
            TextField(
                modifier = Modifier.fillMaxWidth(),
                value = query,
                onValueChange = { v -> query = v; onQueryChange(v) },
                enabled = queryEnabled,
                placeholder = {
                    Text(
                        text = stringResource(com.madness.collision.R.string.sdk_search_hint),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        fontSize = 14.sp,
                        lineHeight = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                leadingIcon = {
                    Icon(
                        modifier = Modifier.padding(start = 4.dp),
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        // the user might be typing after clearing the query,
                        // leave the focus and keyboard as they are
                        IconButton(onClick = { query = ""; onQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Outlined.Cancel,
                                contentDescription = null,
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text, imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ),
            )
        }

        if (headerType == 1) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                DeviceApiInfo(
                    modifier = Modifier.weight(3f).fillMaxHeight(),
                    label = devInfoLabel,
                    desc = devInfoDesc,
                    onClick = onClickDevInfo,
                )
                Spacer(modifier = Modifier.width(10.dp))
                ListStats(
                    modifier = Modifier.weight(2f).fillMaxHeight(),
                    sizeLabel = statsSizeLabel,
                    onClick = onClickStats
                )
            }
        }
    }
}

@Composable
private fun DeviceApiInfo(modifier: Modifier = Modifier, label: String, desc: String, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .clip(AbsoluteSmoothCornerShape(15.dp, 60))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .padding(vertical = 10.dp)
                .size(width = 30.dp, height = 50.dp)
                .border(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(5.dp)
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Android,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.padding(vertical = 1.dp)) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                fontSize = 13.sp,
                lineHeight = 14.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = desc,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                fontSize = 11.sp,
                lineHeight = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ListStats(modifier: Modifier = Modifier, sizeLabel: String, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .clip(AbsoluteSmoothCornerShape(15.dp, 60))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp).height(50.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.PieChart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Stats",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
            fontSize = 13.sp,
            lineHeight = 14.sp,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = sizeLabel,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            fontSize = 11.sp,
            lineHeight = 12.sp,
        )
    }
}

@Composable
private fun TextLabelAction(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    titleLabel: String,
    valueLabel: String,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.padding(vertical = 5.dp).size(12.dp),
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            modifier = Modifier.weight(1f),
            text = titleLabel,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
            fontSize = 12.sp,
            lineHeight = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = valueLabel,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            fontSize = 11.sp,
            lineHeight = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@PreviewCombinedColorLayout
@Composable
private fun HeaderPreview() {
    BoundoTheme {
        AppListHeader(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 5.dp),
            devInfoLabel = "Android 15",
            devInfoDesc = "API 35, Vanilla Ice Cream",
            statsSizeLabel = "231",
            onClickDevInfo = { },
            onClickStats = { },
            queryEnabled = true,
            onQueryChange = {},
        )
    }
}