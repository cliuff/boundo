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

package com.madness.collision.versatile

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.madness.collision.R
import com.madness.collision.main.MainActivity
import com.madness.collision.unit.Unit
import com.madness.collision.unit.api_viewing.AccessAV
import com.madness.collision.util.P
import com.madness.collision.util.ThemeUtil
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

internal class ApkSharing: AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtil.updateTheme(this, getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE))
        setContentView(R.layout.fragment_loading)
        GlobalScope.launch {
            val extras = intent.extras
            if (extras == null) {
                finish()
                return@launch
            }
            val res: Uri? = extras.getParcelable(Intent.EXTRA_STREAM)
            if (res == null) {
                finish()
                return@launch
            }
            val args = Bundle()
            args.putInt(AccessAV.EXTRA_LAUNCH_MODE, AccessAV.LAUNCH_MODE_LINK)
            args.putParcelable(AccessAV.EXTRA_DATA_STREAM, res)
            Intent(this@ApkSharing, MainActivity::class.java).run {
                putExtras(MainActivity.forItem(Unit.UNIT_NAME_API_VIEWING, args))
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(this)
            }
            finish()
        }
    }
}
