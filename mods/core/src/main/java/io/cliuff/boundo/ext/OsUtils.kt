/*
 * Copyright 2021 Clifford Liu
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

package io.cliuff.boundo.ext

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

object OsUtils {
    const val Baklava = Build.VERSION_CODES.BAKLAVA
    const val V = Build.VERSION_CODES.VANILLA_ICE_CREAM
    const val U = Build.VERSION_CODES.UPSIDE_DOWN_CAKE
    const val T = Build.VERSION_CODES.TIRAMISU
    const val S_V2 = Build.VERSION_CODES.S_V2
    const val S = Build.VERSION_CODES.S
    const val R = Build.VERSION_CODES.R
    const val Q = Build.VERSION_CODES.Q
    const val P = Build.VERSION_CODES.P
    const val O_MR1 = Build.VERSION_CODES.O_MR1
    const val O = Build.VERSION_CODES.O
    const val N_MR1 = Build.VERSION_CODES.N_MR1
    const val N = Build.VERSION_CODES.N
    const val M = Build.VERSION_CODES.M

    @ChecksSdkIntAtLeast(parameter = 0)
    fun satisfy(apiLevel: Int): Boolean {
        return Build.VERSION.SDK_INT >= apiLevel
    }
}