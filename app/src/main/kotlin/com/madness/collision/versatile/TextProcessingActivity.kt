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
import com.madness.collision.chief.app.ComposePageActivity
import com.madness.collision.unit.api_viewing.AccessAV

@RequiresApi(Build.VERSION_CODES.M)
class TextProcessingActivity : ComposePageActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
        val readOnly = intent.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true)
        // set process result
        if (text != null && !readOnly) {
            val result = Intent().putExtra(Intent.EXTRA_PROCESS_TEXT, text)
            setResult(Activity.RESULT_OK, result)
        }
        val route = AccessAV.getAppListRoute(query = text)
        intent = intent.putExtra(EXTRA_ROUTE, route)
        super.onCreate(savedInstanceState)
    }
}

class ApiViewingSearchActivity : ComposePageActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)
        val route = AccessAV.getAppListRoute(query = text)
        intent = intent.putExtra(EXTRA_ROUTE, route)
        super.onCreate(savedInstanceState)
    }
}
