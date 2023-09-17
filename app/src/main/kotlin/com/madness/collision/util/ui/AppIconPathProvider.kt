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

import android.graphics.Bitmap
import android.graphics.Path
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toAndroidRectF
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.madness.collision.chief.chiefContext
import com.madness.collision.chief.graphics.MiuiIconCustomizer
import com.madness.collision.util.F
import com.madness.collision.util.ImageUtil
import com.madness.collision.util.X
import com.madness.collision.util.dev.idString
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

interface AppIconPathProvider {
    enum class Type { MaskPath, MaskBitmap }
    fun getType(): Type = Type.MaskPath
    fun getPath(path: Path, size: Int, offset: Int)
    fun getMaskBitmap(size: Int, offset: Int): Bitmap? = null
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

class MiuiMaskPathProvider : AppIconPathProvider {
    private val cacheProvider = CacheProvider<String, Bitmap>()
    private val fallback = SmoothCornerPathProvider()
    override fun getType() = AppIconPathProvider.Type.MaskBitmap
    override fun getPath(path: Path, size: Int, offset: Int) = fallback.getPath(path, size, offset)

    override fun getMaskBitmap(size: Int, offset: Int): Bitmap? {
        val iconMask = MiuiIconCustomizer.getIconMaskBitmap() ?: return null
        // iconMask is subject to change, use object id + size as key
        val cacheKey = "${iconMask.idString}/$size"
        return cacheProvider.get(cacheKey) { X.toTarget(iconMask, size, size) }
    }
}

/** Provide mask bitmap from local file, with SmoothCornerPathProvider as fallback */
class LocalMaskPathProvider : AppIconPathProvider {
    private val iconMask: Bitmap? by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        val mask = F.createFile(F.valCachePubAvLogo(chiefContext), "mask", "icon_mask.png")
        if (mask.exists()) ImageUtil.getBitmap(mask) else null
    }
    private val cacheProvider = CacheProvider<Int, Bitmap>()
    private val fallback = SmoothCornerPathProvider()
    override fun getType() = AppIconPathProvider.Type.MaskBitmap
    override fun getPath(path: Path, size: Int, offset: Int) = fallback.getPath(path, size, offset)

    override fun getMaskBitmap(size: Int, offset: Int): Bitmap? {
        val mask = iconMask ?: return null
        // iconMask will not change once initialized, use size as key
        return cacheProvider.get(size) { X.toTarget(mask, size, size) }
    }
}

class CacheProvider<K, V> {
    val cacheMap = LinkedHashMap<K, V>()
    val cacheLock = Any()
    
    inline fun get(key: K, cache: () -> V & Any): V & Any {
        cacheMap[key]?.let { return it }
        return synchronized(cacheLock) sync@{
            cacheMap[key]?.let { return@sync it }
            cache().also {
                if (cacheMap.size >= 12) {
                    val iterator = cacheMap.iterator()
                    for (i in 1..7) {
                        if (iterator.hasNext().not()) break
                        iterator.remove()
                    }
                }
                cacheMap[key] = it
            }
        }
    }
}
