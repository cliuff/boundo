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
import android.os.Build
import android.text.PrecomputedText
import android.text.Spannable
import android.text.SpannableString
import android.text.style.LeadingMarginSpan
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.PrecomputedTextCompat
import androidx.core.view.updateMarginsRelative
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.madness.collision.R
import com.madness.collision.main.MainApplication
import com.madness.collision.main.MainViewModel
import com.madness.collision.util.file.ContentProviderUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.lang.ref.WeakReference
import kotlin.math.roundToInt

/**
 * extensions
 */
val mainApplication = MainApplication.INSTANCE

/**
 * pre-compute text and set text when computing work is finished
 * 19040222
 */
infix fun TextView.dart(text: CharSequence) = dartText(this, text)

private fun dartText(view: TextView, text: CharSequence) {
    val ref = WeakReference(view)
    GlobalScope.launch {
        if (X.aboveOn(X.P)) {
            val text4View = PrecomputedText.create(text, view.textMetricsParams)
            GlobalScope.launch(Dispatchers.Main) {
                ref.get()?.let { tv ->
                    tv.textMetricsParams = text4View.params
                    tv.text = text4View
                }
            }
        } else {
            val text4View = PrecomputedTextCompat.create(text, TextViewCompat.getTextMetricsParams(view))
            GlobalScope.launch(Dispatchers.Main) {
                ref.get()?.let { tv ->
                    TextViewCompat.setTextMetricsParams(tv, text4View.params)
                    TextViewCompat.setPrecomputedText(tv, text4View)
                }
            }
        }
    }
}

/**
 * pre-computed text boost performance but cause bug, cannot measure accurately,
 * set a character in order to measure
 * 19040222
 */
infix fun TextView.dartHold(text: CharSequence) {
    this.text = P.DART_PLACEHOLDER
    this dart text
}

/**
 * IllegalArgumentException: Given text can not be applied to TextView
 * Meizu 15 Plus, API 24
 * [TextViewCompat.setPrecomputedText]
 *
 * IllegalArgumentException: PrecomputedText's Parameters don't match the parameters of this TextView.
 * Consider using setTextMetricsParams(precomputedText.getParams()) to override the settings of this TextView.
 * meizu 17 Pro, API 30
 */
infix fun AppCompatTextView.dartFuture(text: CharSequence) {
    if (Build.MANUFACTURER.toLowerCase(SystemUtil.getLocaleApp()) == "meizu") {
        setText(text)
        return
    }
    val textFuture = try {
        PrecomputedTextCompat.getTextFuture(text, textMetricsParamsCompat, null)
    } catch (e: IllegalArgumentException) {
        e.printStackTrace()
        setText(text)
        return
    }
    setTextFuture(textFuture)
}

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
        // todo accurate occupiedWidth calculation, currently affected by side nav view inflation
        val occupiedWidth = mainViewModel.sideNavWidth
        val hasSideNav = occupiedWidth != 0
        return X.getCurrentAppResolution(context).x - occupiedWidth - (mainViewModel.insetRight.value ?: 0) - if (hasSideNav) 0 else (mainViewModel.insetLeft.value ?: 0)
    }

fun File.getProviderUri(context: Context): Uri = ContentProviderUtils.getUri(context, this)

val Bitmap.collisionBitmap: Bitmap
        get() = if (!this.isMutable || (X.aboveOn(X.O) && config == Bitmap.Config.HARDWARE)) this.copy(Bitmap.Config.ARGB_8888, true) else this

inline fun runnable(crossinline task: Runnable.() -> Unit) = object : Runnable {
    override fun run() = this.task()
}

fun BottomSheetBehavior<*>.configure(context: Context) {
    val preservedHeight = X.size(context, 120f, X.DP).roundToInt()
    val totalHeight = X.getCurrentAppResolution(context).y
    val heightLeft = totalHeight / 5
    peekHeight = totalHeight - (if (heightLeft >= preservedHeight) heightLeft else preservedHeight)
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
