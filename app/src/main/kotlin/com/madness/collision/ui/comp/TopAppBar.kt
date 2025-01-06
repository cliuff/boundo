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

package com.madness.collision.ui.comp

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import com.madness.collision.chief.app.LocalPageNavController
import com.madness.collision.util.mainApplication

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassicTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
) {
    val appBarColor = when (LocalInspectionMode.current) {
        true -> if (isSystemInDarkTheme()) Color(0xff0c0c0c) else Color.White
        false -> if (mainApplication.isDarkTheme) Color(0xff0c0c0c) else Color.White
    }
    val appBarDividerColor = when (LocalInspectionMode.current) {
        true -> if (isSystemInDarkTheme()) Color(0xFF505050) else Color(0xFFC0C0C0)
        false -> if (mainApplication.isDarkTheme) Color(0xFF505050) else Color(0xFFC0C0C0)
    }
    val appBarIconColor = when (LocalInspectionMode.current) {
        true -> if (isSystemInDarkTheme()) Color(0xFFD0D6DB) else Color(0xFF353535)
        false -> if (mainApplication.isDarkTheme) Color(0xFFD0D6DB) else Color(0xFF353535)
    }
    Column {
        val navController = LocalPageNavController.current
        CenterAlignedTopAppBar(
            modifier = modifier,
            title = title,
            navigationIcon = {
                IconButton(onClick = navController::navigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = null,
                        tint = appBarIconColor,
                    )
                }
            },
            actions = actions,
            windowInsets = windowInsets,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = appBarColor.copy(alpha = 0.87f)),
        )
        HorizontalDivider(
            thickness = 0.5.dp,
            color = appBarDividerColor.copy(alpha = 0.3f),
        )
    }
}
