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
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.madness.collision.util.notice.ToastUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun notify(textResId: Int, shouldLastLong: Boolean, shouldDelay: Boolean, view: View? = null,
                   context: Context? = null) {
    if (view != null) {
        val dur = if (shouldLastLong) Snackbar.LENGTH_LONG else Snackbar.LENGTH_SHORT
        val snackbar = Snackbar.make(view, textResId, dur)
        if (shouldDelay) {
            GlobalScope.launch {
                delay(800)
                snackbar.show()
            }
        } else {
            snackbar.show()
        }
    } else {
        context ?: return
        GlobalScope.launch {
            val length = if (shouldLastLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
            ToastUtils.toast(context, textResId, length)
        }
    }
}

private fun Fragment.notify(textResId: Int, shouldLastLong: Boolean, shouldDelay: Boolean) {
    val activity = this.activity
    if (activity != null) {
        // show on top to avoid being shadowed under bottom nav view
        activity.notify(textResId, shouldLastLong, shouldDelay)
    } else {
        val view = this.view
        val context = this.context
        notify(textResId, shouldLastLong, shouldDelay, view, context)
    }
}

fun Fragment.notify(textResId: Int, shouldDelay: Boolean = false) {
    this.notify(textResId, shouldLastLong = true, shouldDelay = shouldDelay)
}

fun Fragment.notifyBriefly(textResId: Int, shouldDelay: Boolean = false) {
    this.notify(textResId, shouldLastLong = false, shouldDelay = shouldDelay)
}

private fun ComponentActivity.notify(textResId: Int, shouldLastLong: Boolean, shouldDelay: Boolean) {
    val view = this.window.decorView.rootView
    val context = this
    notify(textResId, shouldLastLong, shouldDelay, view, context)
}

fun ComponentActivity.notify(textResId: Int, shouldDelay: Boolean = false) {
    this.notify(textResId, shouldLastLong = true, shouldDelay = shouldDelay)
}

fun ComponentActivity.notifyBriefly(textResId: Int, shouldDelay: Boolean = false) {
    this.notify(textResId, shouldLastLong = false, shouldDelay = shouldDelay)
}

private fun View.notify(textResId: Int, shouldLastLong: Boolean, shouldDelay: Boolean) {
    val view = this
    val context = this.context
    notify(textResId, shouldLastLong, shouldDelay, view, context)
}

fun View.notify(textResId: Int, shouldDelay: Boolean = false) {
    this.notify(textResId, shouldLastLong = true, shouldDelay = shouldDelay)
}

fun View.notifyBriefly(textResId: Int, shouldDelay: Boolean = false) {
    this.notify(textResId, shouldLastLong = false, shouldDelay = shouldDelay)
}

private fun Context.notify(textResId: Int, shouldLastLong: Boolean, shouldDelay: Boolean) {
    val context = this
    notify(textResId, shouldLastLong, shouldDelay, context = context)
}

fun Context.notify(textResId: Int, shouldDelay: Boolean = false) {
    this.notify(textResId, shouldLastLong = true, shouldDelay = shouldDelay)
}

fun Context.notifyBriefly(textResId: Int, shouldDelay: Boolean = false) {
    this.notify(textResId, shouldLastLong = false, shouldDelay = shouldDelay)
}
