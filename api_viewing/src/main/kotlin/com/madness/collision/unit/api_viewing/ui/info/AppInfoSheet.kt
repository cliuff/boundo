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

package com.madness.collision.unit.api_viewing.ui.info

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import com.madness.collision.chief.app.rememberColorScheme
import com.madness.collision.unit.api_viewing.data.ApiViewingApp

@Stable
interface AppInfoEventHandler {
    fun shareAppIcon(app: ApiViewingApp)
    fun shareAppArchive(app: ApiViewingApp)
}

val LocalAppInfoCallback = compositionLocalOf<AppInfoFragment.Callback?> { null }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppInfoSheet(
    onDismissRequest: () -> Unit,
    eventHandler: AppInfoEventHandler,
    state: AppInfoSheetState = rememberAppInfoState(),
    appInfoCallback: AppInfoFragment.Callback? = LocalAppInfoCallback.current,
    contentWindowInsets: @Composable () -> WindowInsets = { BottomSheetDefaults.windowInsets },
) {
    if (state.app != null) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            dragHandle = null,
            contentWindowInsets = contentWindowInsets,
        ) {
            // bottom sheet's content is inside a separate window, with independent window insets
            BoxWithConstraints {
                val contentPadding = contentWindowInsets().asPaddingValues()
                val sheetHeight = (maxHeight * 0.8f)
                    .coerceIn(600.dp, 800.dp)
                    .coerceAtMost(maxHeight - contentPadding.calculateTopPadding() - 10.dp)

                Box(modifier = Modifier.heightIn(max = sheetHeight)) {
                    AppInfoPageContent(
                        appInfoState = state,
                        appInfoCallback = appInfoCallback,
                        eventHandler = eventHandler,
                        colorScheme = rememberColorScheme(),
                    )
                }
            }
        }
    }
}
