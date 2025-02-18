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

package com.madness.collision.chief.config

import android.util.DisplayMetrics
import androidx.compose.ui.unit.Dp
import androidx.core.util.TypedValueCompat

fun Dp.toPx(metrics: DisplayMetrics): Float {
    // always use platform API TypedValue.applyDimension() to account for custom scaling
    return TypedValueCompat.dpToPx(value, metrics)
}