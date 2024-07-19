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

package com.madness.collision.util.os

import android.os.Build

object OsUtils {
    const val DEV = Build.VERSION_CODES.CUR_DEVELOPMENT
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
    const val L_MR1 = Build.VERSION_CODES.LOLLIPOP_MR1
    const val L = Build.VERSION_CODES.LOLLIPOP
    const val K_WATCH = Build.VERSION_CODES.KITKAT_WATCH
    const val K = Build.VERSION_CODES.KITKAT
    const val J_MR2 = Build.VERSION_CODES.JELLY_BEAN_MR2
    const val J_MR1 = Build.VERSION_CODES.JELLY_BEAN_MR1
    const val J = Build.VERSION_CODES.JELLY_BEAN
    const val I_MR1 = Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
    const val I = Build.VERSION_CODES.ICE_CREAM_SANDWICH
    const val H_MR2 = Build.VERSION_CODES.HONEYCOMB_MR2
    const val H_MR1 = Build.VERSION_CODES.HONEYCOMB_MR1
    const val H = Build.VERSION_CODES.HONEYCOMB
    const val G_MR1 = Build.VERSION_CODES.GINGERBREAD_MR1
    const val G = Build.VERSION_CODES.GINGERBREAD
    const val F = Build.VERSION_CODES.FROYO
    const val E_MR1 = Build.VERSION_CODES.ECLAIR_MR1
    const val E_0_1 = Build.VERSION_CODES.ECLAIR_0_1
    const val E = Build.VERSION_CODES.ECLAIR
    const val D = Build.VERSION_CODES.DONUT
    const val C = Build.VERSION_CODES.CUPCAKE
    const val B = Build.VERSION_CODES.BASE_1_1
    const val A = Build.VERSION_CODES.BASE

    fun satisfy(apiLevel: Int): Boolean {
        return Build.VERSION.SDK_INT >= apiLevel
    }

    fun dissatisfy(apiLevel: Int): Boolean {
        return Build.VERSION.SDK_INT < apiLevel
    }
}