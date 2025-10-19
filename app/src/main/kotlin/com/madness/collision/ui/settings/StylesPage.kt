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

package com.madness.collision.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.compose.AndroidFragment
import com.madness.collision.R
import com.madness.collision.chief.layout.scaffoldWindowInsets
import com.madness.collision.pref.PrefExterior
import com.madness.collision.ui.comp.ClassicTopAppBar
import com.madness.collision.ui.theme.MetaAppTheme

@Composable
fun StylesPage() {
    val (topBarInsets, _, contentInsets) =
        scaffoldWindowInsets(shareCutout = 12.dp, shareStatusBar = 5.dp, shareWaterfall = 8.dp)
    Scaffold(
        topBar = {
            ClassicTopAppBar(
                title = { Text(text = stringResource(R.string.settings_exterior)) },
                windowInsets = topBarInsets
                    .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
            )
        },
        containerColor = MetaAppTheme.colorScheme.surfaceNeutral,
        contentWindowInsets = contentInsets,
    ) { innerPadding ->
        StylesContent(contentPadding = innerPadding)
    }
}

@Composable
fun StylesContent(contentPadding: PaddingValues = PaddingValues.Zero) {
    Box(modifier = Modifier.padding(contentPadding)) {
        AndroidFragment<PrefExterior>(modifier = Modifier.fillMaxWidth())
    }
}
