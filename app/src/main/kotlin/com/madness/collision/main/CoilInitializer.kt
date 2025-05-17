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

package com.madness.collision.main

import androidx.core.util.TypedValueCompat
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.crossfade
import com.madness.collision.util.ui.AppIconFetcher
import com.madness.collision.util.ui.AppIconKeyer
import kotlin.math.roundToInt

object CoilInitializer : SingletonImageLoader.Factory {
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val metrics = context.resources.displayMetrics
        val iconSize = TypedValueCompat.dpToPx(48f, metrics).roundToInt()
        return ImageLoader.Builder(context)
            .crossfade(true)
            .components {
                add(AppIconKeyer(context))
                add(AppIconFetcher.Factory(iconSize, false, context))
            }
            .build()
    }
}