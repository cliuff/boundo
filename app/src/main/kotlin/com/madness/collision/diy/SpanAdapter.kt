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

package com.madness.collision.diy

import android.content.Context
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.madness.collision.util.X
import com.madness.collision.util.alterMargin
import com.madness.collision.util.availableWidth
import kotlin.math.roundToInt

interface SpanAdapter {
    companion object {
        const val COL_POS_SINGLE = -1
        const val COL_POS_START = 0
        const val COL_POS_CENTER = 1
        const val COL_POS_END = 2
    }

    val context: Context
    var spanCount: Int

    fun suggestLayoutManager(): RecyclerView.LayoutManager {
        return if (spanCount == 1) LinearLayoutManager(context) else GridLayoutManager(context, spanCount)
    }

    fun resolveSpanCount(fragment: Fragment, unit: Float, width: () -> Int = {
        fragment.availableWidth
    }) {
        val unitWidth = X.size(context, unit, X.DP)
        spanCount = (width.invoke() / unitWidth).roundToInt().run {
            if (this < 2) 1 else this
        }
    }

    fun getColumnIndex(index: Int): Int {
        return index % spanCount
    }

    fun getColumnPosition(index: Int): Int {
        if (spanCount < 2) return COL_POS_SINGLE
        return when (getColumnIndex(index)) {
            0 -> COL_POS_START
            spanCount - 1 -> COL_POS_END
            else -> COL_POS_CENTER
        }
    }

    fun optimizeSideMargin(index: Int, outer: Float, inner: Float, view: View) {
        val sideMargins = when (getColumnPosition(index)) {
            COL_POS_START -> outer to inner
            COL_POS_CENTER -> inner to inner
            COL_POS_END -> inner to outer
            else -> outer to outer
        }
        val marginStart = X.size(context, sideMargins.first, X.DP).roundToInt()
        val marginEnd = X.size(context, sideMargins.second, X.DP).roundToInt()
        view.alterMargin(start = marginStart, end = marginEnd)
    }
}