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

package com.madness.collision.chief.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Deprecated(
    message = "Use MetaAppTheme or PreviewAppTheme instead.",
    replaceWith = ReplaceWith("MetaAppTheme", "com.madness.collision.ui.theme.MetaAppTheme")
)
@Composable
fun BoundoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = rememberColorScheme(), content = content)
}
