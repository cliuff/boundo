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

package com.madness.collision.unit.we_chat_evo

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.madness.collision.R
import com.madness.collision.util.*

class InstantWeChatActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.let {
            SystemUtil.applyEdge2Edge(it)
            SystemUtil.applyStatusBarColor(this, it, false, isTransparentBar = true)
            SystemUtil.applyNavBarColor(this, it, false, isTransparentBar = true)
        }
        setContentView(R.layout.activity_instant_wechat)
        val root: ConstraintLayout = findViewById(R.id.instantWeChatRoot)
        root.background = if (ThemeUtil.getIsNight(this)) ColorDrawable(ThemeUtil.getColor(this, R.attr.colorABackground))
        else getDrawable(R.drawable.ic_tencent_mm_back_a)
        val bar: ProgressBar = findViewById(R.id.instant_wechat_bar)
        bar.postDelayed({
            try {
                startActivity(packageManager.getLaunchIntentForPackage("com.tencent.mm"))
                if (X.belowOff(X.P)) overridePendingTransition(R.anim.res_fade_in, R.anim.res_fade_out)
                finish()
            }catch( e: Exception){
                notifyBriefly(R.string.WeChatLauncher_Launch_Fail)
            }
        }, 400)
    }
}
