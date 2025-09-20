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

package com.madness.collision.qs

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.madness.collision.main.MainActivity
import com.madness.collision.unit.Unit
import com.madness.collision.util.X

internal class PrefActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.action != TileService.ACTION_QS_TILE_PREFERENCES) {
            finish()
            return
        }
        val cn: ComponentName? = if (X.aboveOn(X.O)) intent.getParcelableExtra(Intent.EXTRA_COMPONENT_NAME) else null
        if (cn != null) {
            when(cn.className) {
                TileServiceAudioTimer::class.qualifiedName -> {
                    Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        putExtras(MainActivity.forItem(Unit.UNIT_NAME_AUDIO_TIMER))
                    }
                }
                else -> {
                    X.toast(this, "2333", Toast.LENGTH_SHORT)
                    null
                }
            }?.let { startActivity(it) }
        } else {
            X.toast(this, "2333", Toast.LENGTH_SHORT)
        }
        finish()
    }

}
