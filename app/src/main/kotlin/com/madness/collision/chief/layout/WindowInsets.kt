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

package com.madness.collision.chief.layout

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.runtime.compositionLocalOf
import androidx.core.view.WindowInsetsCompat

/**
 * [Exclude][exclude] [insets] from this [WindowInsets].
 *
 * Share insets with component's padding to improve unity (e.g. status bar and app bar).
 */
fun WindowInsets.share(insets: WindowInsets): WindowInsets = exclude(insets)

val LocalWindowInsets = compositionLocalOf<WindowInsetsCompat> { error("Insets not provided.") }
