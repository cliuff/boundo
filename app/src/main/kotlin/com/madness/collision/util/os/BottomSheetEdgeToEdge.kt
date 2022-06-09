/*
 * Copyright 2022 Clifford Liu
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

import android.content.Context
import android.view.View
import android.view.Window
import androidx.core.view.WindowInsetsCompat
import com.madness.collision.R
import com.madness.collision.diy.WindowInsets
import com.madness.collision.util.ColorUtil
import com.madness.collision.util.SystemUtil
import com.madness.collision.util.ThemeUtil

class BottomSheetEdgeToEdge(
    sysMan: SystemBarMaintainerOwner,
    private val consumeInsets: (WindowInsets) -> Unit,
    private val getWindow: () -> Window?,
) : SystemBarMaintainerOwner by sysMan {

    fun applyInsets(view: View, context: Context) {
        view.setOnApplyWindowInsetsListener { v, insets ->
            if (checkInsets(insets)) {
                configEdgeToEdge(insets, context, getWindow())
                val isRtl = if (v.isLayoutDirectionResolved) v.layoutDirection == View.LAYOUT_DIRECTION_RTL else false
                consumeInsets(WindowInsets(insets, isRtl))
            }
            WindowInsetsCompat.CONSUMED.toWindowInsets()
        }
    }

    private fun configEdgeToEdge(insets: android.view.WindowInsets, context: Context, window: Window?): Boolean {
        var isEdgeToEdge = false //= true
        edgeToEdge(insets, false) {
            // keep status bar icon color untouched
            // Actually this works as intended only when app theme is set to follow system,
            // not configuring this icon color makes it follow dialog's style/theme,
            // which is defined in styles.xml and it follows system dark mode setting.
            // To fix this, set it to the window config of the activity before this.
            top {
                // fix status bar icon color
                window?.let { isDarkIcon = SystemUtil.isDarkStatusIcon(window) }
            }
            bottom {
                val colorSurface = ThemeUtil.getColor(context, R.attr.colorASurface)
                isEdgeToEdge = (isDarkIcon == true && OsUtils.dissatisfy(OsUtils.O)).not()
                color = if (isEdgeToEdge) {
                    colorSurface
                } else {
                    ColorUtil.darkenAs(colorSurface, 0.9f)
                }
                transparentBar()
            }
        }
        return isEdgeToEdge
    }
}