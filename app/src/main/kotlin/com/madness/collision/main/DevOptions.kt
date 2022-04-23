/*
 * Copyright 2022 Clifford Liu
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

package com.madness.collision.main

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.madness.collision.R
import com.madness.collision.main.more.DisplayInfo
import com.madness.collision.unit.api_viewing.AccessAV
import com.madness.collision.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class DevOptions(private val scope: CoroutineScope) {
    private val devOptions: List<Pair<String, (Context) -> Unit>> = listOf(
        "Immortal" to { context ->
            val intent = Intent(context, ImmortalActivity::class.java).apply {
                putExtra(P.IMMORTAL_EXTRA_LAUNCH_MODE, P.IMMORTAL_EXTRA_LAUNCH_MODE_MORTAL)
            }
            context.startActivity(intent)
        },
        "Display info" to { context ->
            scope.launch(Dispatchers.Default) {
                val displayInfo = DisplayInfo.getDisplaysAndInfo(context)
                withContext(Dispatchers.Main) {
                    showInfoDialog(context, displayInfo)
                }
            }
        },
        "App Room info" to { context ->
            scope.launch(Dispatchers.Default) {
                val roomInfo = AccessAV.getRoomInfo(context)
                withContext(Dispatchers.Main) {
                    showInfoDialog(context, roomInfo)
                }
            }
        },
        "Clean App Room" to { context ->
            scope.launch(Dispatchers.Default) {
                AccessAV.clearRoom(context)
                withContext(Dispatchers.Main) {
                    context.notifyBriefly(R.string.text_done)
                }
            }
        },
        "Nuke App Room" to { context ->
            scope.launch(Dispatchers.Default) {
                val re = AccessAV.nukeAppRoom(context)
                withContext(Dispatchers.Main) {
                    context.notifyBriefly(if (re) R.string.text_done else R.string.text_error)
                }
            }
        },
    )

    fun show(context: Context) {
        val customContent = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        val pop = CollisionDialog(context, R.string.text_OK).apply {
            setContent(0)
            setTitleCollision(0, 0, 0)
            setCustomContentMere(customContent)
            setListener { dismiss() }
        }
        val marginHor = X.size(context, 20f, X.DP).roundToInt()
        val marginVer = X.size(context, 4f, X.DP).roundToInt()
        val btnTint = ThemeUtil.getColor(context, R.attr.colorAItem).let { ColorStateList.valueOf(it) }
        val btnTextColor = ThemeUtil.getColor(context, R.attr.colorAOnItem).let { ColorStateList.valueOf(it) }
        devOptions.forEach { (title, click) ->
            val btn = MaterialButton(context).apply {
                text = title
                setTextColor(btnTextColor)
                backgroundTintList = btnTint
                isAllCaps = false
                setOnClickListener {
                    pop.dismiss()
                    click(context)
                }
            }
            customContent.addView(btn)
            btn.alterMargin(start = marginHor, top = marginVer, end = marginHor, bottom = marginVer)
        }
        customContent.alterPadding(top = marginHor)
        pop.decentHeight()
        pop.show()
    }

    private fun showInfoDialog(context: Context, content: CharSequence) {
        val view = TextView(context).apply {
            text = content
            textSize = 8f
            val padding = X.size(context, 6f, X.DP).roundToInt()
            alterPadding(start = padding, top = padding * 3, end = padding)
        }
        CollisionDialog(context, R.string.text_OK).apply {
            setContent(0)
            setTitleCollision(0, 0, 0)
            setCustomContent(view)
            decentHeight()
            setListener { dismiss() }
        }.show()
    }
}