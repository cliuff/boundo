/*
 * Copyright 2024 Clifford Liu
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

package com.madness.collision.unit.api_viewing.ui.upd

import android.view.animation.OvershootInterpolator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt

private val OvershootEasing: Easing = Easing(OvershootInterpolator()::getInterpolation)

@Composable
fun rememberOvershootRotation(rotating: Boolean): State<Float> {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(rotating) {
        if (rotating) {
            val rotateSpec = tween<Float>(1000, easing = LinearEasing)
            while (isActive) {
                anim.animateTo(360f, rotateSpec)
                anim.snapTo(0f)
            }
        } else {
            val progress = anim.value % 360f
            if (progress != 0f) {
                val duration = ((360f - progress) * 1000f / 360f).roundToInt()
                val overshootSpec = tween<Float>(duration, easing = OvershootEasing)
                anim.animateTo(360f, overshootSpec)
                anim.snapTo(0f)
            }
        }
    }
    return anim.asState()
}
