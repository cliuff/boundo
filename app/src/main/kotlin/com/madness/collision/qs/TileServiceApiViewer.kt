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

import android.annotation.TargetApi
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import com.madness.collision.main.MainActivity
import com.madness.collision.unit.Unit

@TargetApi(Build.VERSION_CODES.N)
class TileServiceApiViewer : TileService() {
    override fun onTileAdded() {
        TileCommon.inactivate(qsTile)
    }

    override fun onStartListening() {
        TileCommon.inactivate(qsTile)
    }

    override fun onClick() {
        Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtras(MainActivity.forItem(Unit.UNIT_NAME_API_VIEWING))
            startActivityAndCollapse(this)
        }
    }
}
