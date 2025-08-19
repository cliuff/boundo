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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewSquare
import io.cliuff.boundo.wear.model.ApiViewingApp
import io.cliuff.boundo.wear.ui.theme.PreviewAppTheme

@Composable
internal fun ArtItem(
    app: ApiViewingApp,
    modifier: Modifier = Modifier,
    transformation: SurfaceTransformation? = null,
) {
    ArtItemContainer(modifier = modifier, onClick = {}, transformation = transformation) {
        ArtItemContent(
            name = app.name,
            time = "3 hours ago",
            apiText = app.targetAPI.toString(),
            apiColor = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ArtItemContainer(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    transformation: SurfaceTransformation? = null,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues.Zero,
        transformation = transformation,
    ) {
        Box(
            modifier = Modifier.heightIn(min = CardDefaults.Height),
            contentAlignment = Alignment.CenterStart,
            content = { content() }
        )
    }
}

@Composable
private fun ArtItemContent(
    name: String,
    time: String?,
    apiText: String,
    apiColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .padding(start = 8.dp, end = 8.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Medium,
                overflow = TextOverflow.Ellipsis,
                maxLines = 2,
            )
            if (time != null) {
                Text(
                    modifier = Modifier.padding(top = 2.dp),
                    text = time,
                    fontSize = 8.sp,
                    lineHeight = 8.sp,
                    fontWeight = FontWeight.Medium,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
            }
        }

        Box(modifier = Modifier.padding(2.dp).widthIn(min = 24.dp)) {
            Text(
                modifier = Modifier.align(Alignment.CenterStart),
                text = apiText,
                color = apiColor,
                fontSize = 18.sp,
                lineHeight = 20.sp,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }
    }
}

@WearPreviewDevices
@WearPreviewSquare
@Composable
private fun ArtItemPreview() {
    PreviewAppTheme {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
            ArtItemContainer(modifier = Modifier.padding(horizontal = 12.dp), onClick = {}) {
                ArtItemContent(
                    name = "Boundo Meta App",
                    time = "3 hours ago",
                    apiText = "16",
                    apiColor = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
