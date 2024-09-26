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

package com.madness.collision.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.net.Uri
import android.text.Spannable
import android.text.SpannableString
import android.text.style.LeadingMarginSpan
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.updateMarginsRelative
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.madness.collision.R
import com.madness.collision.main.MainApplication
import com.madness.collision.main.MainViewModel
import com.madness.collision.util.file.ContentProviderUtils
import java.io.File
import kotlin.math.roundToInt

/**
 * extensions
 */
val mainApplication = MainApplication.INSTANCE

fun View.alterPadding(
        start: Int = this.paddingStart,
        top: Int = this.paddingTop,
        end: Int = this.paddingEnd,
        bottom: Int = this.paddingBottom
) = setPadding(start, top, end, bottom)

fun View.alterMargin(
        start: Int = (layoutParams as ViewGroup.MarginLayoutParams).marginStart,
        top: Int = (layoutParams as ViewGroup.MarginLayoutParams).topMargin,
        end: Int = (layoutParams as ViewGroup.MarginLayoutParams).marginEnd,
        bottom: Int = (layoutParams as ViewGroup.MarginLayoutParams).bottomMargin) {
    val params = layoutParams as ViewGroup.MarginLayoutParams
    params.updateMarginsRelative(start, top, end, bottom)
}

fun View.measure(shouldLimitHor: Boolean = false, shouldLimitVer: Boolean = false) {
    val msHor: Int
    val msVer: Int
    var p: Point? = null
    if (shouldLimitHor || shouldLimitVer) p = X.getCurrentAppResolution(context)
    msHor = if (shouldLimitHor) View.MeasureSpec.makeMeasureSpec(p!!.x * 4 / 5, View.MeasureSpec.AT_MOST) else View.MeasureSpec.UNSPECIFIED
    msVer = if (shouldLimitVer) View.MeasureSpec.makeMeasureSpec(p!!.y, View.MeasureSpec.AT_MOST) else View.MeasureSpec.UNSPECIFIED
    measure(msHor, msVer)
}

val Context.spanLittleMore
    get() = this.resources.getBoolean(R.bool.spanLittleMore)

val Context.spanJustMore
    get() = this.resources.getBoolean(R.bool.spanJustMore)

val Fragment.availableWidth: Int
    get() {
        val context = context ?: return 0
        val mainViewModel: MainViewModel by activityViewModels()
        val extraInsetStart = mainViewModel.insetStart.value ?: 0
        val extraInsetEnd = mainViewModel.insetEnd.value ?: 0
        return X.getCurrentAppResolution(context).x - extraInsetEnd - extraInsetStart
    }

fun File.getProviderUri(context: Context): Uri = ContentProviderUtils.getUri(context, this)

fun Bitmap.toMutable(): Bitmap = GraphicsUtil.toMutable(this)

inline fun runnable(crossinline task: Runnable.() -> Unit) = object : Runnable {
    // SAM cannot access *this*
    override fun run() = this.task()
}

fun BottomSheetBehavior<*>.configure(context: Context) {
    val preservedHeight = X.size(context, 120f, X.DP).roundToInt()
    val windowSize = SystemUtil.getRuntimeWindowSize(context)
    val totalHeight = windowSize.y
    val heightLeft = totalHeight / 5
    peekHeight = (totalHeight - (if (heightLeft >= preservedHeight) heightLeft else preservedHeight))
        .coerceAtMost(X.size(context, 800f, X.DP).roundToInt())
    // reserve 40dp padding for 640dp max width, otherwise set width to the largest
    if (windowSize.x <= X.size(context, 680f, X.DP).roundToInt()) {
        maxWidth = windowSize.x
    }
}

fun Fragment.ensureAdded(containerId: Int, fragment: Fragment, isCommitNow: Boolean = false) {
    if (!fragment.isAdded) childFragmentManager.beginTransaction().add(containerId, fragment).run {
        if (isCommitNow) commitNow() else commit()
    }
}

private const val LEADING_MARGIN_INDENT = 30f

fun CharSequence.leadingMargin(context: Context, indentDP: Float = LEADING_MARGIN_INDENT): CharSequence {
    val indent = X.size(context, indentDP, X.DP).roundToInt()
    val span = LeadingMarginSpan.Standard(indent, 0)
    val spannable = SpannableString(this)
    spannable.setSpan(span, 0, spannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    return spannable
}

fun Int.leadingMargin(context: Context, indentDP: Float = LEADING_MARGIN_INDENT): CharSequence {
    return context.getString(this).leadingMargin(context, indentDP)
}

fun TextView.setMarginText(context: Context, text: CharSequence, indentDP: Float = LEADING_MARGIN_INDENT) {
    this.text = text.leadingMargin(context, indentDP)
}

fun TextView.setMarginText(context: Context, textResId: Int, indentDP: Float = LEADING_MARGIN_INDENT) {
    text = textResId.leadingMargin(context, indentDP)
}
