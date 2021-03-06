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

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.madness.collision.main.MainActivity
import com.madness.collision.unit.Unit
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.M)
class TextProcessingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var flagAction = false
        var actionIntent: Intent? = null
        GlobalScope.launch {
            val extras = intent.extras
            if (extras == null){
                finish()
                return@launch
            }
            val text = extras.getCharSequence(Intent.EXTRA_PROCESS_TEXT) ?: ""
            val readOnly = extras.getBoolean(Intent.EXTRA_PROCESS_TEXT_READONLY, true)
            if (text.isEmpty()) {
                finish()
                return@launch
            }
            if (!readOnly){
                Intent().run {
                    putExtra(Intent.EXTRA_PROCESS_TEXT, text)
                    setResult(Activity.RESULT_OK, this)
                }
            }
            val args = Bundle()
            args.putString(Intent.EXTRA_TEXT, text.toString())
            actionIntent = Intent(this@TextProcessingActivity, MainActivity::class.java).apply {
                putExtras(MainActivity.forItem(Unit.UNIT_NAME_API_VIEWING, args))
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            flagAction = true
        }.invokeOnCompletion {
            if (!flagAction) return@invokeOnCompletion
            startActivity(actionIntent)
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0){
            setResult(Activity.RESULT_OK)
            finish()
        }
    }
}
