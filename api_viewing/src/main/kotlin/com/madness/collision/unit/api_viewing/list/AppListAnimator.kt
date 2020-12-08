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

package com.madness.collision.unit.api_viewing.list

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import androidx.core.animation.doOnEnd
import com.madness.collision.R
import com.madness.collision.unit.api_viewing.data.ApiViewingApp

internal class AppListAnimator {

    fun animateLogo(logoView: ImageView) {
        val animFade = ObjectAnimator.ofFloat(logoView, "alpha",1f, 0.1f).apply {
            interpolator = AccelerateDecelerateInterpolator()
            duration = 100
            doOnEnd {
                val app = logoView.getTag(R.bool.tagKeyAvAdapterItemId) as ApiViewingApp
                logoView.setImageBitmap(app.icon)
            }
        }
        val animShow = ObjectAnimator.ofFloat(logoView, "alpha",0.1f, 1f).apply {
            interpolator = AccelerateDecelerateInterpolator()
            duration = 100
        }

        AnimatorSet().run {
            play(animFade).before(animShow)
            start()
        }
    }
}