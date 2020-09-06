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
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.madness.collision.R
import com.madness.collision.unit.audio_timer.AccessAT
import com.madness.collision.unit.audio_timer.AtCallback
import com.madness.collision.util.X
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@TargetApi(Build.VERSION_CODES.N)
class TileServiceAudioTimer : TileService() {
    private var mCallback: AtCallback? = null
    override fun onTileAdded() {
        TileCommon.inactivate(qsTile)
        updateState()
    }

    override fun onStartListening() {
        updateState()
    }

    override fun onClick() {
        val isRunning = AccessAT.isRunning()
        if (isRunning) {
            AccessAT.stop(this)
            TileCommon.inactivate(qsTile)
        } else {
            AccessAT.start(this)
            qsTile.state = Tile.STATE_ACTIVE
            qsTile.updateTile()
        }
    }

    private fun updateState() {
        val isRunning = AccessAT.isRunning()
        qsTile.state = if (isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        qsTile.updateTile()
        if (isRunning) {
            if (mCallback == null) {
                object : AtCallback {
                    override fun onTick(targetTime: Long, duration: Long, leftTime: Long) {
                        if (leftTime > 500L) return
                        GlobalScope.launch {
                            delay(1000)
                            updateState()
                        }
                    }
                    override fun onTick(displayText: String) {
                        if (X.aboveOn(X.Q)) {
                            qsTile.subtitle = displayText
                        } else {
                            qsTile.label = displayText
                        }
                        qsTile.updateTile()
                    }
                }.let {
                    mCallback = it
                    AccessAT.addCallback(it)
                }
            } else {
                AccessAT.removeCallback(mCallback)
                AccessAT.addCallback(mCallback)
            }
        } else {
            if (X.aboveOn(X.Q)) {
                qsTile.subtitle = null
            } else {
                qsTile.label = baseContext.getString(R.string.unit_audio_timer)
            }
            qsTile.updateTile()
            AccessAT.removeCallback(mCallback)
            mCallback = null
        }
    }

}
