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

package com.madness.collision.unit.audio_timer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import com.madness.collision.R
import com.madness.collision.databinding.UnitAudioTimerBinding
import com.madness.collision.unit.Unit
import com.madness.collision.util.MathUtils.boundMin
import com.madness.collision.util.P
import com.madness.collision.util.X
import com.madness.collision.util.alterMargin
import com.madness.collision.util.alterPadding
import com.madness.collision.util.os.OsUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MyUnit : Unit() {

    override val id: String = "AT"

    private var _viewBinding: UnitAudioTimerBinding? = null
    private val viewBinding: UnitAudioTimerBinding
        get() = _viewBinding!!

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        configNavigation(toolbar, iconColor)
        toolbar.setTitle(R.string.unit_audio_timer)
        return true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _viewBinding = UnitAudioTimerBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onDestroyView() {
        _viewBinding = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = context ?: return
        val pref = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
        val timeHour = pref.getInt(P.AT_TIME_HOUR, 0)
        val timeMinute = pref.getInt(P.AT_TIME_MINUTE, 0)
        if (timeHour != 0) viewBinding.atHour.setText(timeHour.toString())
        if (timeMinute != 0) viewBinding.atMinute.setText(timeMinute.toString())
        updateStatus()
        viewBinding.atStart.setOnClickListener {
            // stop if running already
            if (AudioTimerService.isRunning) {
                val intent = Intent(context, AudioTimerService::class.java)
                context.stopService(intent)
                GlobalScope.launch {
                    delay(100)
                    launch(Dispatchers.Main) {
                        updateStatus()
                    }
                }
                return@setOnClickListener
            }
            kotlin.run p@{
                // request runtime permission on Android 13
                if (OsUtils.dissatisfy(OsUtils.T)) return@p
                if (NotificationManagerCompat.from(context).areNotificationsEnabled()) return@p
                postNotificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return@setOnClickListener
            }
            startTimer(context)
        }
    }

    private val postNotificationsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()) register@{ granted ->
        if (!granted) return@register
        val context = context ?: return@register
        startTimer(context)
    }

    private fun startTimer(context: Context) {
        val hourInput = viewBinding.atHour.text?.toString() ?: ""
        val targetHour = if (hourInput.isEmpty()) 0 else hourInput.toInt()
        val minuteInput = viewBinding.atMinute.text?.toString() ?: ""
        val targetMinute = if (minuteInput.isEmpty()) 0 else minuteInput.toInt()
        val targetDuration = (targetHour * 60 + targetMinute) * 60000L
        val intent = Intent(context, AudioTimerService::class.java)
        context.stopService(intent)
        intent.putExtra(AudioTimerService.ARG_DURATION, targetDuration)
        context.startService(intent)
        GlobalScope.launch {
            delay(100)
            launch(Dispatchers.Main) {
                updateStatus()
            }
        }
        val shouldUpdateHour = hourInput.isNotEmpty()
        val shouldUpdateMinute = minuteInput.isNotEmpty()
        val pref = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
        pref.edit {
            if (shouldUpdateHour) putInt(P.AT_TIME_HOUR, targetHour)
            else remove(P.AT_TIME_HOUR)
            if (shouldUpdateMinute) putInt(P.AT_TIME_MINUTE, targetMinute)
            else remove(P.AT_TIME_MINUTE)
        }
    }

    private fun updateStatus() {
        val icon = if (AudioTimerService.isRunning) R.drawable.ic_clear_24 else R.drawable.ic_arrow_forward_24
        viewBinding.atStart.setImageResource(icon)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        democratize()
        val context = context ?: return
        val minMargin = X.size(context, 80f, X.DP).roundToInt()
        val gapMargin = X.size(context, 30f, X.DP).roundToInt()
        mainViewModel.contentWidthBottom.observe(viewLifecycleOwner) {
            val margin = it + gapMargin
            viewBinding.atStart.alterMargin(bottom = margin.boundMin(minMargin))
            // update view
            viewBinding.atStart.requestLayout()
        }
        mainViewModel.contentWidthTop.observe(viewLifecycleOwner) {
            viewBinding.atContainer.alterPadding(top = it)
        }
    }
}