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

package io.cliuff.boundo.wear.ui.list

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import io.cliuff.boundo.wear.R
import io.cliuff.boundo.wear.model.ApiViewingApp

@Composable
internal fun LazyAppGrid(
    apps: List<ApiViewingApp>,
    columnState: TransformingLazyColumnState = rememberTransformingLazyColumnState(),
    contentPadding: PaddingValues = PaddingValues.Zero,
) {
    val transSpec = rememberTransformationSpec()

    TransformingLazyColumn(state = columnState, contentPadding = contentPadding) {

        item(key = "@list.header", contentType = "Header") {
            ListHeader(
                modifier = Modifier.transformedHeight(this, transSpec),
                transformation = SurfaceTransformation(transSpec),
            ) {
                Text(stringResource(R.string.art_list_hrader))
            }
        }

        items(apps, key = { app -> app.packageName }, contentType = { "App" }) { app ->
            ArtItem(
                modifier = Modifier
                    .animateItem()
                    .transformedHeight(this, transSpec),
                transformation = SurfaceTransformation(transSpec),
                app = app,
            )
        }
    }
}
