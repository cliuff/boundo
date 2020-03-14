package com.madness.collision.wearable.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.net.Uri
import android.text.PrecomputedText
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.FileProvider
import androidx.core.text.PrecomputedTextCompat
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.Fragment
import com.madness.collision.wearable.BuildConfig
import com.madness.collision.wearable.main.MainApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.lang.ref.WeakReference

/**
 * extensions
 */
internal val mainApplication = MainApplication.INSTANCE

/**
 * pre-compute text and set text when computing work is finished
 * 19040222
 */
internal infix fun TextView.dart(text: CharSequence) = dartText(this, text)

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
internal infix fun TextView.dartHold(text: CharSequence) {
    this.text = P.DART_PLACEHOLDER
    this dart text
}

internal infix fun AppCompatTextView.dartFuture(text: CharSequence) {
    setTextFuture(PrecomputedTextCompat.getTextFuture(text, textMetricsParamsCompat, null))
}

internal fun View.alterPadding(
        start: Int = this.paddingStart,
        top: Int = this.paddingTop,
        end: Int = this.paddingEnd,
        bottom: Int = this.paddingBottom
) = setPadding(start, top, end, bottom)

internal fun View.measure(shouldLimitHor: Boolean = false, shouldLimitVer: Boolean = false) {
    val msHor: Int
    val msVer: Int
    var p: Point? = null
    if (shouldLimitHor || shouldLimitVer) p = X.getCurrentAppResolution(context)
    msHor = if (shouldLimitHor) View.MeasureSpec.makeMeasureSpec(p!!.x * 4 / 5, View.MeasureSpec.AT_MOST) else View.MeasureSpec.UNSPECIFIED
    msVer = if (shouldLimitVer) View.MeasureSpec.makeMeasureSpec(p!!.y, View.MeasureSpec.AT_MOST) else View.MeasureSpec.UNSPECIFIED
    measure(msHor, msVer)
}

internal val Fragment.availableWidth: Int
    get() {
        val context = context ?: return 0
        return X.getCurrentAppResolution(context).x
    }

internal fun File.getProviderUri(context: Context): Uri{
    return if (X.aboveOn(X.N)) {
        FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileProvider", this)
    } else {
        Uri.fromFile(this)
    }
}

internal val Bitmap.collisionBitmap: Bitmap
        get() = if (!this.isMutable || (X.aboveOn(X.O) && config == Bitmap.Config.HARDWARE)) this.copy(Bitmap.Config.ARGB_8888, true) else this
