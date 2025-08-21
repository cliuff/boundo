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

package io.cliuff.boundo.wear.ui.comp

import android.text.format.DateUtils
import io.cliuff.boundo.os.OS

object ArtMapper {
    fun getRelativeTime(targetTime: Long): String {
        val now = System.currentTimeMillis()
        return DateUtils.getRelativeTimeSpanString(
            targetTime, now, DateUtils.MINUTE_IN_MILLIS).toString()
    }

    fun getItemColorAccent(os: OS?): Int =
        when (os) {
            OS.Baklava -> 0xffa3d1d5
            OS.V -> 0xffbde1a4
            OS.U -> 0xffa3c1d5
            OS.T -> 0xffa3d5c1
            OS.S, OS.S_V2 -> 0xffacdcb2
            OS.R -> 0xffacd5c1
            OS.Q -> 0xffc1d5ac
            OS.P -> 0xffe0c8b0
            OS.O, OS.O_MR1 -> 0xffb0b0b0
            OS.N, OS.N_MR1 -> 0xffffb2a8
            OS.M -> 0xffb0c9c5
            OS.L, OS.L_MR1 -> 0xffffb0b0
            OS.K, OS.K_WATCH -> 0xffd0c7ba
            OS.J, OS.J_MR1, OS.J_MR2 -> 0xffbaf5ba
            OS.I, OS.I_MR1 -> 0xffd8d0c0
            OS.H, OS.H_MR1, OS.H_MR2 -> 0xfff0c8b4
            else -> 0xffc5e8b0
        }.toInt()
}
