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

package com.madness.collision.unit.we_chat_evo

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.madness.collision.R
import com.madness.collision.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InstantWeChatActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtil.updateTheme(this, getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE))
        window?.let { SystemUtil.applyEdge2Edge(it) }
        setContentView(R.layout.activity_instant_wechat)
        window?.let {
            val isDarkIcon = mainApplication.isPaleTheme
            SystemUtil.applyStatusBarColor(this, it, isDarkIcon, isTransparentBar = true)
            SystemUtil.applyNavBarColor(this, it, isDarkIcon, isTransparentBar = true)
        }
        lifecycleScope.launch(Dispatchers.Default) {
            delay(200)
            withContext(Dispatchers.Main) {
                launchWeChat()
            }
        }
    }

    private fun launchWeChat() {
        val launchIntent = packageManager.getLaunchIntentForPackage("com.tencent.mm")?.apply {
            addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            sourceBounds = intent?.sourceBounds
        }
        if (launchIntent == null) {
            notifyBriefly(R.string.WeChatLauncher_Launch_Fail)
            return
        }
        val block = {
            val anim = ActivityOptions.makeCustomAnimation(this,
                    android.R.anim.fade_in, android.R.anim.fade_out)
            startActivity(launchIntent, anim.toBundle())
            finish()
        }
        try {
            block.invoke()
        } catch (e: NullPointerException) {
            // java.lang.NullPointerException: Attempt to invoke virtual method
            // 'boolean com.android.server.wm.ActivityRecord.isVisible()' on a null object reference
            // on Pixel 2 XL Android 11 when previously used Android conversation bubbles
            e.printStackTrace()
            try {
                block.invoke()
            } catch (e: Exception) {
                e.printStackTrace()
                notifyBriefly(R.string.WeChatLauncher_Launch_Fail)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            notifyBriefly(R.string.WeChatLauncher_Launch_Fail)
        }
    }
}
