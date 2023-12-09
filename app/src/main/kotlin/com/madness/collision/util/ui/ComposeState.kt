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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/** Delay the first state update */
@Composable
fun <T> State<T>.initDelayed(millis: Long): State<T> {
    val originalState = this
    if (millis <= 0) return originalState
    var isDelayFinished by remember { mutableStateOf(false) }
    return when {
        isDelayFinished -> originalState
        else -> produceState(originalState.value) {
            delay(millis)
            if (!isDelayFinished) {
                isDelayFinished = true
                value = originalState.value
            }
        }
    }
}
