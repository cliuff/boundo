/*
 * Copyright 2023 Clifford Liu
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

package com.madness.collision.util.ui

import android.graphics.Path
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toAndroidRectF
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.madness.collision.chief.chiefContext
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

interface AppIconPathProvider {
    fun getPath(path: Path, size: Int, offset: Int)
}

class RoundedCornerPathProvider : AppIconPathProvider {
    override fun getPath(path: Path, size: Int, offset: Int) {
        // custom corner ratio: 20dp/100dp = 0.2
        val cornerRadius = (size - 2f * offset) * 0.2f
        path.addRoundRect(offset.toFloat(), offset.toFloat(), (size - offset).toFloat(),
            (size - offset).toFloat(), cornerRadius, cornerRadius, Path.Direction.CW)
    }
}

class SmoothCornerPathProvider : AppIconPathProvider {
    override fun getPath(path: Path, size: Int, offset: Int) {
        val shapeSize = size - 2f * offset
        val density = Density(chiefContext)
        // custom corner ratio: 20dp/100dp = 0.2
        val radiusDp = with(density) { (shapeSize * 0.2f).toDp() }
        val shape = AbsoluteSmoothCornerShape(radiusDp, 80)
        when (val o = shape.createOutline(Size(shapeSize, shapeSize), LayoutDirection.Ltr, density)) {
            is Outline.Rectangle -> o.rect.translate(offset.toFloat(), offset.toFloat()).toAndroidRectF()
                .let { path.addRect(it, Path.Direction.CW) }
            is Outline.Rounded -> Unit  // not implemented
            is Outline.Generic -> path.addPath(o.path.asAndroidPath(), offset.toFloat(), offset.toFloat())
        }
    }
}
