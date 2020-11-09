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

package com.madness.collision.main

import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.animation.doOnEnd

internal class MainAnimator {

    fun hideBottomNavShowing(showBottomNav: View) {
        ObjectAnimator.ofFloat(showBottomNav, "alpha", 1f, 0.01f).apply {
            interpolator = AccelerateDecelerateInterpolator()
            duration = 100
            doOnEnd {
                showBottomNav.visibility = View.GONE
            }
        }.start()
    }

    fun showBottomNavShowing(showBottomNav: View) {
        ObjectAnimator.ofFloat(showBottomNav, "alpha", 0.01f, 1f).apply {
            interpolator = AccelerateDecelerateInterpolator()
            duration = 100
            doOnEnd {
                showBottomNav.visibility = View.VISIBLE
            }
        }.start()
    }
}