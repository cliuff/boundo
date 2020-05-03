/*
 * Copyright 2020 Clifford Liu
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

package com.madness.collision.util

import android.graphics.Bitmap
import android.util.Size
import kotlin.math.max
import kotlin.math.min

//@Suppress("NOTHING_TO_INLINE")
//inline operator fun Size.component1(): Int = width
//@Suppress("NOTHING_TO_INLINE")
//inline operator fun Size.component2(): Int = height

val Bitmap.size: Size
get() = Size(width, height)

infix fun Int.sizeTo(other: Int) = Size(this, other)

fun Pair<Int, Int>.toSize() = Size(first, second)

fun Size.doOptimal(optimalWidth: Int): Size {
    val lengthHorizontal = width
    val lengthVertical = height
    if (max(lengthHorizontal, lengthVertical) == optimalWidth) {
        return this
    }
    var targetWidth: Int = optimalWidth
    var targetHeight: Int = optimalWidth
    when {
        lengthVertical > lengthHorizontal -> {
            targetWidth = (lengthHorizontal * optimalWidth) / lengthVertical
        }
        lengthVertical < lengthHorizontal -> {
            targetHeight = (lengthVertical * optimalWidth) / lengthHorizontal
        }
    }
    return targetWidth sizeTo targetHeight
}

fun Size.doMinimum(minimumWidth: Int): Size {
    val lengthHorizontal = width
    val lengthVertical = height
    if (min(lengthHorizontal, lengthVertical) == minimumWidth) {
        return this
    }
    var targetWidth: Int = minimumWidth
    var targetHeight: Int = minimumWidth
    when {
        lengthVertical > lengthHorizontal -> {
            targetHeight = (lengthVertical * minimumWidth) / lengthHorizontal
        }
        lengthVertical < lengthHorizontal -> {
            targetWidth = (lengthHorizontal * minimumWidth) / lengthVertical
        }
    }
    return targetWidth sizeTo targetHeight
}

object SizeUtil {

}
