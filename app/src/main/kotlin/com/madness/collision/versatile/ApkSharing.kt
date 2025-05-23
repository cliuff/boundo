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

package com.madness.collision.versatile

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.lifecycle.lifecycleScope
import com.madness.collision.R
import com.madness.collision.base.BaseActivity
import com.madness.collision.chief.app.ComposePageActivityIntent
import com.madness.collision.unit.api_viewing.AccessAV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class ApkSharing : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_loading)
        lifecycleScope.launch(Dispatchers.Default) {
            val context = this@ApkSharing
            val apkFile = getFile(context) ?: return@launch
            if (apkFile !is Parcelable) return@launch
            val intent = ComposePageActivityIntent(AccessAV.getAppListRoute(pkgInfo = apkFile))
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            withContext(Dispatchers.Main) {
                startActivity(intent)
                finish()
            }
        }
    }

    private fun getFile(context: Context): Any? {
        val res: Uri = intent.extras?.getParcelable(Intent.EXTRA_STREAM) ?: return null
        // uri access permission expires once leaving this activity
        // if make file copy of content uri, raw file uri cannot be accessed unless copied as well
        // so get package info directly, which is parcelable and more light-weight than AV app
        return AccessAV.resolveUri(context, res)
    }
}
