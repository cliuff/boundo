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

package com.madness.collision.unit.api_viewing.ui.list.comp

import android.content.Context
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import coil3.compose.rememberAsyncImagePainter
import com.madness.collision.unit.api_viewing.Utils
import com.madness.collision.unit.api_viewing.seal.SealMaker
import com.madness.collision.util.F
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

private suspend fun getHeaderImage(context: Context): File? {
    val f = F.createFile(F.valFilePubExterior(context), "Art_ListHeader.jpg")
    if (f.exists()) return f

    val sealIndex = Utils.getDevCodenameLetter()
        ?: Utils.getAndroidLetterByAPI(Build.VERSION.SDK_INT)
    return SealMaker.getBlurredCacheFile(sealIndex) ?: run {
        val itemWidth = 45.dp.value * context.resources.displayMetrics.density
        SealMaker.getBlurredFile(context, sealIndex, itemWidth.roundToInt())
    }
}

@Composable
fun ListHeaderImage(
    height: Dp,
    backdropColor: Color,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val imgFile by produceState(null as File?) {
        value = withContext(Dispatchers.IO) {
            getHeaderImage(context)
        }
    }
    Box(modifier = modifier) {
        Image(
            modifier = Modifier.fillMaxWidth(),
            painter = rememberAsyncImagePainter(imgFile),
            contentDescription = null,
            contentScale = ContentScale.Crop,
        )
        if (height.isSpecified) {
            Column {
                Spacer(modifier = Modifier.fillMaxWidth().height(height))

                Box(modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
                    .background(backdropColor))
            }
        }
    }
}

@Composable
fun ListHeaderVerticalShade(size: Dp) {
    Column {
        Box(modifier = Modifier
            .fillMaxWidth()
            .background(brush = Brush.verticalGradient(
                0f to Color(0x10000000), 1f to Color(0x09000000)))
            .height(size))
        Box(modifier = Modifier
            .fillMaxWidth()
            .background(brush = Brush.verticalGradient(
                0f to Color(0x09000000), 1f to Color(0x00000000)))
            .height(30.dp))
    }
}

@OptIn(ExperimentalHazeApi::class)
@Composable
fun ListHeaderBackdrop(
    color: Color,
    cornerSize: Dp,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit,
) {
    val backdropHeight = 20.dp
    val style = HazeDefaults.style(
        backgroundColor = Color.Transparent,
        tint = HazeTint(Color.Transparent),
    )
    // app list header with blur and backdrop
    Column(
        modifier = modifier
            .fillMaxWidth()
            .hazeEffect(hazeState, style) {
                inputScale = HazeInputScale.Fixed(0.8f)
            }
    ) {
        header()

        val backdropShape = RoundedCornerShape(topStart = cornerSize, topEnd = cornerSize)
        // backdrop for app list, overlaps on the blurred background
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(backdropHeight)
            .background(color, backdropShape))
    }
}
